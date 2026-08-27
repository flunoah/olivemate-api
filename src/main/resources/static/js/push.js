function urlBase64ToUint8Array(base64String) {
    const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
    const rawData = atob(base64);
    return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)));
}

function csrfHeaders(extra) {
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    const token = document.querySelector('meta[name="_csrf"]').content;
    return Object.assign({ [header]: token }, extra || {});
}

function isPushSupported() {
    return "serviceWorker" in navigator && "PushManager" in window;
}

async function getExistingSubscription() {
    if (!isPushSupported()) return null;
    const registration = await navigator.serviceWorker.getRegistration();
    return (await registration?.pushManager.getSubscription()) ?? null;
}

async function subscribeToPush() {
    if (!isPushSupported()) {
        console.warn("[push] subscribe 중단: 이 브라우저는 Web Push를 지원하지 않음");
        return false;
    }

    const permission = await Notification.requestPermission();
    if (permission !== "granted") {
        console.warn("[push] subscribe 중단: 알림 권한이 허용되지 않음");
        return false;
    }

    try {
        const registration = await navigator.serviceWorker.register("/service-worker.js");
        await navigator.serviceWorker.ready;

        const keyRes = await fetch("/push/vapid-public-key");
        if (!keyRes.ok) {
            console.error("[push] subscribe 실패: VAPID 공개키 조회 실패", keyRes.status);
            return false;
        }
        const keyJson = await keyRes.json();
        const publicKey = keyJson.data ?? keyJson;

        let subscription = await registration.pushManager.getSubscription();
        if (!subscription) {
            subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: urlBase64ToUint8Array(publicKey),
            });
        }

        const json = subscription.toJSON();
        const subRes = await fetch("/push/subscribe", {
            method: "POST",
            headers: csrfHeaders({ "Content-Type": "application/json" }),
            body: JSON.stringify({
                endpoint: json.endpoint,
                p256dh: json.keys?.p256dh,
                auth: json.keys?.auth,
            }),
        });

        if (!subRes.ok) {
            console.error("[push] subscribe 실패: 백엔드 구독 저장 실패", subRes.status);
        }
        return subRes.ok;
    } catch (e) {
        console.error("[push] subscribe 실패: 예외 발생", e);
        return false;
    }
}

async function updatePushChannels(notifyPointEarned, notifyPointExpiring, notifyAdminAdjusted) {
    try {
        const res = await fetch("/push/subscribe/channels", {
            method: "PATCH",
            headers: csrfHeaders({ "Content-Type": "application/json" }),
            body: JSON.stringify({ notifyPointEarned, notifyPointExpiring, notifyAdminAdjusted }),
        });
        if (!res.ok) {
            console.error("[push] 채널 설정 저장 실패", res.status);
        }
        return res.ok;
    } catch (e) {
        console.error("[push] 채널 설정 저장 실패: 예외 발생", e);
        return false;
    }
}

async function unsubscribeFromPush() {
    if (!isPushSupported()) return false;
    try {
        const subscription = await getExistingSubscription();
        if (!subscription) return true;

        const endpoint = subscription.endpoint;
        await subscription.unsubscribe();

        const res = await fetch(`/push/subscribe?endpoint=${encodeURIComponent(endpoint)}`, {
            method: "DELETE",
            headers: csrfHeaders(),
        });
        if (!res.ok) {
            console.error("[push] unsubscribe 실패: 백엔드 구독 삭제 실패", res.status);
        }
        return res.ok;
    } catch (e) {
        console.error("[push] unsubscribe 실패: 예외 발생", e);
        return false;
    }
}

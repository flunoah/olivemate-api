CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    crew_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(500) NOT NULL,
    deep_link VARCHAR(500),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_notifications_crew_id ON notifications(crew_id);

CREATE TABLE push_subscriptions (
    id UUID PRIMARY KEY,
    crew_id UUID NOT NULL,
    endpoint VARCHAR(1000) NOT NULL UNIQUE,
    p256dh VARCHAR(500) NOT NULL,
    auth VARCHAR(500) NOT NULL,
    registered_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_push_subscriptions_crew_id ON push_subscriptions(crew_id);
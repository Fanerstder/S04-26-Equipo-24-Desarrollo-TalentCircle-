-- V3__create_community_activities.sql

CREATE TABLE community_activities (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    source_id VARCHAR(255),
    title TEXT NOT NULL,
    content TEXT,
    reaction_count INT DEFAULT 0,
    response_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    author VARCHAR(255),
    published_at TIMESTAMP,
    source_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES weekly_executions(id)
);

CREATE INDEX idx_community_activities_execution_id ON community_activities(execution_id);
CREATE INDEX idx_community_activities_type ON community_activities(type);

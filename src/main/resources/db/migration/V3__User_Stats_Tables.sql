-- V3__User_Stats_Tables.sql
-- Create user_stats table for aggregate statistics

CREATE TABLE user_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- Lifetime totals
    total_focus_minutes INTEGER DEFAULT 0,
    total_sessions_completed INTEGER DEFAULT 0,
    total_sessions_interrupted INTEGER DEFAULT 0,
    total_tasks_completed INTEGER DEFAULT 0,

    -- Streak tracking
    current_streak INTEGER DEFAULT 0,
    best_streak INTEGER DEFAULT 0,
    last_session_date TIMESTAMP,

    -- Level & XP (Phase 2 - Gamification)
    level INTEGER DEFAULT 1,
    current_xp INTEGER DEFAULT 0,
    total_xp_earned INTEGER DEFAULT 0,

    -- Achievements
    achievements_count INTEGER DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_user_stats_user_id ON user_stats(user_id);
CREATE INDEX idx_user_stats_total_focus ON user_stats(total_focus_minutes DESC);
CREATE INDEX idx_user_stats_sessions ON user_stats(total_sessions_completed DESC);
CREATE INDEX idx_user_stats_streak ON user_stats(current_streak DESC);
CREATE INDEX idx_user_stats_level ON user_stats(level DESC, current_xp DESC);

-- Create trigger for updated_at
CREATE TRIGGER update_user_stats_updated_at
    BEFORE UPDATE ON user_stats
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Initialize user_stats for existing users
INSERT INTO user_stats (user_id, total_focus_minutes, total_sessions_completed, total_sessions_interrupted, total_tasks_completed)
SELECT
    u.id,
    COALESCE((
        SELECT SUM(s.duration_minutes)
        FROM sessions s
        WHERE s.user_id = u.id AND s.completed = true
    ), 0) as total_focus_minutes,
    COALESCE((
        SELECT COUNT(*)
        FROM sessions s
        WHERE s.user_id = u.id AND s.completed = true
    ), 0) as total_sessions_completed,
    COALESCE((
        SELECT COUNT(*)
        FROM sessions s
        WHERE s.user_id = u.id AND s.interrupted = true
    ), 0) as total_sessions_interrupted,
    COALESCE((
        SELECT COUNT(*)
        FROM tasks t
        WHERE t.user_id = u.id AND t.completed = true
    ), 0) as total_tasks_completed
FROM users u
ON CONFLICT (user_id) DO NOTHING;

-- Create view for leaderboard queries
CREATE OR REPLACE VIEW leaderboard_view AS
SELECT
    u.id as user_id,
    u.discord_id,
    u.username,
    us.total_focus_minutes,
    us.total_sessions_completed,
    us.current_streak,
    us.level,
    us.current_xp,
    RANK() OVER (ORDER BY us.total_focus_minutes DESC) as rank_by_focus,
    RANK() OVER (ORDER BY us.total_sessions_completed DESC) as rank_by_sessions,
    RANK() OVER (ORDER BY us.current_streak DESC) as rank_by_streak,
    RANK() OVER (ORDER BY us.level DESC, us.current_xp DESC) as rank_by_level
FROM users u
INNER JOIN user_stats us ON u.id = us.user_id
WHERE us.total_sessions_completed > 0;

COMMENT ON TABLE user_stats IS 'Aggregate user statistics for quick access';
COMMENT ON VIEW leaderboard_view IS 'Leaderboard rankings by various metrics';
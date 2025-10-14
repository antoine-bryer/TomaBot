-- V4__XP_System.sql
-- Create XP transaction tracking system

-- Create xp_transactions table
CREATE TABLE xp_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL,
    amount INTEGER NOT NULL,
    level_before INTEGER NOT NULL,
    level_after INTEGER NOT NULL,
    reference_id BIGINT,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_xp_transactions_user_id ON xp_transactions(user_id);
CREATE INDEX idx_xp_transactions_source ON xp_transactions(source);
CREATE INDEX idx_xp_transactions_created_at ON xp_transactions(created_at DESC);
CREATE INDEX idx_xp_transactions_level_up ON xp_transactions(user_id, level_after) 
    WHERE level_after > level_before;

-- Add constraint to ensure positive XP amounts
ALTER TABLE xp_transactions ADD CONSTRAINT chk_xp_amount_positive 
    CHECK (amount >= 0);

-- Add constraint to ensure level progression is valid
ALTER TABLE xp_transactions ADD CONSTRAINT chk_xp_level_valid 
    CHECK (level_before >= 1 AND level_after >= level_before);

-- Create view for XP leaderboard
CREATE OR REPLACE VIEW xp_leaderboard_view AS
SELECT 
    u.id as user_id,
    u.discord_id,
    u.username,
    us.level,
    us.current_xp,
    us.total_xp_earned,
    RANK() OVER (ORDER BY us.level DESC, us.current_xp DESC) as rank,
    COUNT(xt.id) FILTER (WHERE xt.level_after > xt.level_before) as total_level_ups
FROM users u
INNER JOIN user_stats us ON u.id = us.user_id
LEFT JOIN xp_transactions xt ON u.id = xt.user_id
WHERE us.total_xp_earned > 0
GROUP BY u.id, u.discord_id, u.username, us.level, us.current_xp, us.total_xp_earned
ORDER BY us.level DESC, us.current_xp DESC;

-- Create view for XP breakdown by source
CREATE OR REPLACE VIEW xp_source_breakdown_view AS
SELECT 
    u.id as user_id,
    u.discord_id,
    u.username,
    xt.source,
    COUNT(xt.id) as transaction_count,
    SUM(xt.amount) as total_xp,
    AVG(xt.amount) as avg_xp_per_transaction
FROM users u
INNER JOIN xp_transactions xt ON u.id = xt.user_id
GROUP BY u.id, u.discord_id, u.username, xt.source
ORDER BY u.id, total_xp DESC;

-- Create function to calculate XP required for level
CREATE OR REPLACE FUNCTION calculate_xp_for_level(target_level INTEGER)
RETURNS INTEGER AS $$
BEGIN
    IF target_level <= 1 THEN
        RETURN 0;
    END IF;
    RETURN target_level * target_level * 50;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Create function to get user's current level info
CREATE OR REPLACE FUNCTION get_user_level_info(p_user_id BIGINT)
RETURNS TABLE (
    current_level INTEGER,
    current_xp INTEGER,
    xp_for_next_level INTEGER,
    xp_progress_percentage NUMERIC(5,2),
    total_xp_earned INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        us.level,
        us.current_xp,
        calculate_xp_for_level(us.level + 1),
        ROUND((us.current_xp::NUMERIC / calculate_xp_for_level(us.level + 1)::NUMERIC * 100), 2),
        us.total_xp_earned
    FROM user_stats us
    WHERE us.user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE xp_transactions IS 'Tracks all XP gains and level-ups for users';
COMMENT ON VIEW xp_leaderboard_view IS 'Leaderboard of users by level and XP';
COMMENT ON VIEW xp_source_breakdown_view IS 'XP earned breakdown by source for each user';
COMMENT ON FUNCTION calculate_xp_for_level(INTEGER) IS 'Calculate XP required to reach a specific level (level² × 50)';
COMMENT ON FUNCTION get_user_level_info(BIGINT) IS 'Get comprehensive level information for a user';

-- Insert sample data comment for reference
COMMENT ON COLUMN xp_transactions.source IS 'XP source: SESSION_COMPLETED, TASK_COMPLETED, STREAK_BONUS, FIRST_SESSION_OF_DAY, ACHIEVEMENT_UNLOCKED, DAILY_LOGIN, MANUAL_GRANT';
COMMENT ON COLUMN xp_transactions.reference_id IS 'Reference to related entity (session_id, task_id, achievement_id, etc.)';

-- Create trigger to validate XP transactions
CREATE OR REPLACE FUNCTION validate_xp_transaction()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure user_stats exists for the user
    IF NOT EXISTS (SELECT 1 FROM user_stats WHERE user_id = NEW.user_id) THEN
        RAISE EXCEPTION 'User stats must exist before granting XP';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_xp_transaction
    BEFORE INSERT ON xp_transactions
    FOR EACH ROW
    EXECUTE FUNCTION validate_xp_transaction();

-- Grant permissions (if using specific roles)
-- GRANT SELECT ON xp_leaderboard_view TO tomabot;
-- GRANT SELECT ON xp_source_breakdown_view TO tomabot;
-- GRANT EXECUTE ON FUNCTION calculate_xp_for_level(INTEGER) TO tomabot;
-- GRANT EXECUTE ON FUNCTION get_user_level_info(BIGINT) TO tomabot;
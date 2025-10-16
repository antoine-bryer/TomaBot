-- V5__Achievements_Enhanced.sql
-- Enhance achievements table and add comprehensive achievement data

-- Add new columns to achievements table
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS rarity VARCHAR(20) DEFAULT 'COMMON';
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS xp_reward INTEGER DEFAULT 50;
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS is_secret BOOLEAN DEFAULT FALSE;
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS hint VARCHAR(200);
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;

-- Update existing achievements with rarity
UPDATE achievements SET rarity = 'COMMON', xp_reward = 50 WHERE code = 'FIRST_SESSION';
UPDATE achievements SET rarity = 'COMMON', xp_reward = 75 WHERE code = 'EARLY_BIRD';
UPDATE achievements SET rarity = 'UNCOMMON', xp_reward = 150 WHERE code = 'FOCUSED_100';
UPDATE achievements SET rarity = 'RARE', xp_reward = 300 WHERE code = 'MARATHONER';
UPDATE achievements SET rarity = 'EPIC', xp_reward = 500 WHERE code = 'WEEKLY_WARRIOR';
UPDATE achievements SET rarity = 'UNCOMMON', xp_reward = 125 WHERE code = 'TASK_MASTER';

-- Delete old achievements to recreate with proper data
DELETE FROM achievements;

-- Insert comprehensive achievement set
INSERT INTO achievements (code, name, description, icon, requirement_type, requirement_value, rarity, xp_reward, is_secret, hint, display_order) VALUES

-- ===== PROGRESSION ACHIEVEMENTS (Common) =====
('FIRST_FOCUS', 'First Focus', 'Complete your first Pomodoro session', '🌱', 'SESSIONS_COMPLETED', 1, 'COMMON', 50, FALSE, NULL, 1),
('GETTING_STARTED', 'Getting Started', 'Complete 5 focus sessions', '🐣', 'SESSIONS_COMPLETED', 5, 'COMMON', 75, FALSE, NULL, 2),
('EARLY_BIRD', 'Early Bird', 'Complete 10 focus sessions', '🐦', 'SESSIONS_COMPLETED', 10, 'COMMON', 100, FALSE, NULL, 3),
('DEDICATED', 'Dedicated', 'Complete 25 focus sessions', '💪', 'SESSIONS_COMPLETED', 25, 'UNCOMMON', 150, FALSE, NULL, 4),
('COMMITTED', 'Committed', 'Complete 50 focus sessions', '🔥', 'SESSIONS_COMPLETED', 50, 'UNCOMMON', 200, FALSE, NULL, 5),
('CENTURY_FOCUS', 'Century Focus', 'Complete 100 focus sessions', '💯', 'SESSIONS_COMPLETED', 100, 'RARE', 300, FALSE, NULL, 6),
('MILESTONE_250', 'Quarter Master', 'Complete 250 focus sessions', '⭐', 'SESSIONS_COMPLETED', 250, 'RARE', 400, FALSE, NULL, 7),
('MILESTONE_500', 'Half Millennium', 'Complete 500 focus sessions', '🌟', 'SESSIONS_COMPLETED', 500, 'EPIC', 600, FALSE, NULL, 8),
('MILLENNIUM', 'Millennium Master', 'Complete 1000 focus sessions', '👑', 'SESSIONS_COMPLETED', 1000, 'LEGENDARY', 1000, FALSE, NULL, 9),

-- ===== FOCUS TIME ACHIEVEMENTS =====
('FIRST_HOUR', 'First Hour', 'Accumulate 60 minutes of focus time', '⏰', 'TOTAL_FOCUS_MINUTES', 60, 'COMMON', 50, FALSE, NULL, 10),
('TEN_HOURS', 'Ten Hours Club', 'Accumulate 600 minutes of focus time', '⏳', 'TOTAL_FOCUS_MINUTES', 600, 'UNCOMMON', 150, FALSE, NULL, 11),
('MARATHONER', 'Marathoner', 'Accumulate 1000 minutes of focus time', '🏃', 'TOTAL_FOCUS_MINUTES', 1000, 'RARE', 300, FALSE, NULL, 12),
('ULTRA_RUNNER', 'Ultra Runner', 'Accumulate 5000 minutes of focus time', '🏃‍♂️', 'TOTAL_FOCUS_MINUTES', 5000, 'EPIC', 600, FALSE, NULL, 13),
('FOCUS_LEGEND', 'Focus Legend', 'Accumulate 10000 minutes of focus time', '🏆', 'TOTAL_FOCUS_MINUTES', 10000, 'LEGENDARY', 1000, FALSE, NULL, 14),

-- ===== STREAK ACHIEVEMENTS =====
('THREE_DAY_STREAK', '3-Day Streak', 'Maintain a 3-day focus streak', '🔥', 'STREAK_DAYS', 3, 'COMMON', 75, FALSE, NULL, 20),
('WEEKLY_WARRIOR', 'Weekly Warrior', 'Maintain a 7-day focus streak', '📅', 'STREAK_DAYS', 7, 'UNCOMMON', 200, FALSE, NULL, 21),
('FORTNIGHT_FOCUS', 'Fortnight Focus', 'Maintain a 14-day focus streak', '💪', 'STREAK_DAYS', 14, 'RARE', 400, FALSE, NULL, 22),
('MONTHLY_MASTER', 'Monthly Master', 'Maintain a 30-day focus streak', '📆', 'STREAK_DAYS', 30, 'EPIC', 700, FALSE, NULL, 23),
('UNSTOPPABLE', 'Unstoppable', 'Maintain a 60-day focus streak', '⚡', 'STREAK_DAYS', 60, 'LEGENDARY', 1200, FALSE, NULL, 24),
('ETERNAL_FLAME', 'Eternal Flame', 'Maintain a 100-day focus streak', '🔥', 'STREAK_DAYS', 100, 'MYTHIC', 2000, FALSE, NULL, 25),

-- ===== TASK ACHIEVEMENTS =====
('FIRST_TASK', 'Task Tackler', 'Complete your first task', '✅', 'TASKS_COMPLETED', 1, 'COMMON', 50, FALSE, NULL, 30),
('TEN_TASKS', 'Task Warrior', 'Complete 10 tasks', '📝', 'TASKS_COMPLETED', 10, 'COMMON', 100, FALSE, NULL, 31),
('TASK_MASTER', 'Task Master', 'Complete 50 tasks', '📋', 'TASKS_COMPLETED', 50, 'UNCOMMON', 200, FALSE, NULL, 32),
('HUNDRED_TASKS', 'Centurion of Tasks', 'Complete 100 tasks', '💼', 'TASKS_COMPLETED', 100, 'RARE', 350, FALSE, NULL, 33),
('TASK_LEGEND', 'Task Legend', 'Complete 500 tasks', '🏅', 'TASKS_COMPLETED', 500, 'EPIC', 700, FALSE, NULL, 34),

-- ===== LEVEL ACHIEVEMENTS =====
('LEVEL_5', 'Novice Focuser', 'Reach Level 5', '🌟', 'LEVEL_REACHED', 5, 'COMMON', 100, FALSE, NULL, 40),
('LEVEL_10', 'Skilled Focuser', 'Reach Level 10', '⭐', 'LEVEL_REACHED', 10, 'UNCOMMON', 200, FALSE, NULL, 41),
('LEVEL_25', 'Expert Focuser', 'Reach Level 25', '💫', 'LEVEL_REACHED', 25, 'RARE', 400, FALSE, NULL, 42),
('LEVEL_50', 'Master Focuser', 'Reach Level 50', '🌠', 'LEVEL_REACHED', 50, 'EPIC', 700, FALSE, NULL, 43),
('LEVEL_100', 'Legendary Focuser', 'Reach Level 100', '👑', 'LEVEL_REACHED', 100, 'LEGENDARY', 1500, FALSE, NULL, 44),

-- ===== TIME-BASED ACHIEVEMENTS =====
('MORNING_PERSON', 'Morning Person', 'Complete 5 sessions before noon', '🌅', 'MORNING_SESSIONS', 5, 'UNCOMMON', 150, FALSE, NULL, 50),
('NIGHT_OWL', 'Night Owl', 'Complete 5 sessions after 10 PM', '🌙', 'EVENING_SESSIONS', 5, 'UNCOMMON', 150, FALSE, NULL, 51),

-- ===== SPECIAL/SECRET ACHIEVEMENTS =====
('CHRISTMAS_SPIRIT', 'Christmas Spirit', 'Complete a session on Christmas Day', '🎄', 'SPECIAL_DATE', 1, 'RARE', 300, TRUE, 'Focus during a festive holiday', 60),
('SPOOKY_FOCUS', 'Spooky Focus', 'Complete a session on Halloween', '🎃', 'SPECIAL_DATE', 1, 'RARE', 300, TRUE, 'Focus on a spooky night', 61),
('NEW_YEAR_FOCUS', 'New Year Focus', 'Complete a session on New Year''s Day', '🎆', 'SPECIAL_DATE', 1, 'RARE', 300, TRUE, 'Start the year with focus', 62),
('PERFECT_WEEK', 'Perfect Week', 'Complete at least one session every day for 7 days', '💎', 'PERFECT_WEEK', 1, 'EPIC', 500, FALSE, 'Be consistent for a whole week', 63);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_achievements_rarity ON achievements(rarity);
CREATE INDEX IF NOT EXISTS idx_achievements_type ON achievements(requirement_type);
CREATE INDEX IF NOT EXISTS idx_achievements_enabled ON achievements(is_enabled);
CREATE INDEX IF NOT EXISTS idx_achievements_order ON achievements(display_order);

-- Create view for achievement statistics
CREATE OR REPLACE VIEW achievement_unlock_stats AS
SELECT
    a.id,
    a.code,
    a.name,
    a.rarity,
    COUNT(ua.id) as total_unlocks,
    MIN(ua.unlocked_at) as first_unlock,
    MAX(ua.unlocked_at) as latest_unlock,
    ROUND(COUNT(ua.id)::NUMERIC / NULLIF((SELECT COUNT(*) FROM users), 0) * 100, 2) as unlock_percentage
FROM achievements a
LEFT JOIN user_achievements ua ON a.id = ua.achievement_id
WHERE a.is_enabled = TRUE
GROUP BY a.id, a.code, a.name, a.rarity
ORDER BY total_unlocks DESC;

-- Add comments
COMMENT ON TABLE achievements IS 'Enhanced achievements with rarity, XP rewards, and secret badges';
COMMENT ON COLUMN achievements.rarity IS 'Achievement rarity: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC';
COMMENT ON COLUMN achievements.xp_reward IS 'Base XP reward (total XP = base + rarity bonus)';
COMMENT ON COLUMN achievements.is_secret IS 'Secret achievements are hidden until unlocked or close to unlock';
COMMENT ON COLUMN achievements.hint IS 'Hint displayed for secret achievements';
COMMENT ON VIEW achievement_unlock_stats IS 'Statistics on how many users have unlocked each achievement';
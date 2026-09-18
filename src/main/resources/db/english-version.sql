-- English-only seed-data conversion for the existing diet_db database.
-- Review and run this manually in your MySQL client after backing up the database.
-- It changes the shared tag dictionary and the built-in public meal collection.
-- It intentionally preserves users' personal meal names and historical chat content.

START TRANSACTION;

UPDATE diet_slot_option
SET option_value = CASE slot_name
    WHEN 'mealTime' THEN CASE option_value
        WHEN '早餐' THEN 'Breakfast' WHEN '早午餐' THEN 'Brunch'
        WHEN '午餐' THEN 'Lunch' WHEN '下午茶' THEN 'Afternoon tea'
        WHEN '晚餐' THEN 'Dinner' WHEN '夜宵' THEN 'Late-night meal'
        WHEN '加餐' THEN 'Snack' WHEN '三餐' THEN 'All-day meals' ELSE option_value END
    WHEN 'mood' THEN CASE option_value
        WHEN '疲惫' THEN 'Tired' WHEN '烦躁' THEN 'Irritable' WHEN '开心' THEN 'Happy'
        WHEN '焦虑' THEN 'Anxious' WHEN '低落' THEN 'Low' WHEN '平静' THEN 'Calm'
        WHEN '压力大' THEN 'Stressed' WHEN '没胃口' THEN 'No appetite'
        WHEN '想放松' THEN 'Want to relax' WHEN '想奖励自己' THEN 'Want a treat' ELSE option_value END
    WHEN 'scene' THEN CASE option_value
        WHEN '工作' THEN 'Work' WHEN '校园' THEN 'Campus' WHEN '家里' THEN 'Home'
        WHEN '周末' THEN 'Weekend' WHEN '加班' THEN 'Overtime' WHEN '运动后' THEN 'After exercise'
        WHEN '通勤' THEN 'Commute' WHEN '聚餐' THEN 'Group meal' WHEN '独处' THEN 'Alone'
        WHEN '旅行' THEN 'Travel' WHEN '夜宵' THEN 'Late night' ELSE option_value END
    WHEN 'healthGoal' THEN CASE option_value
        WHEN '减脂' THEN 'Weight loss' WHEN '清淡' THEN 'Light' WHEN '养胃' THEN 'Gentle on stomach'
        WHEN '高蛋白' THEN 'High protein' WHEN '均衡' THEN 'Balanced' WHEN '降火' THEN 'Cooling foods'
        WHEN '低油' THEN 'Low oil' WHEN '低盐' THEN 'Low salt' WHEN '低糖' THEN 'Low sugar'
        WHEN '补能' THEN 'Energy boost' WHEN '增肌' THEN 'Muscle gain' WHEN '控碳水' THEN 'Low carb'
        WHEN '易消化' THEN 'Easy to digest' WHEN '暖胃' THEN 'Warm and comforting' ELSE option_value END
    WHEN 'cuisine' THEN CASE option_value
        WHEN '川菜' THEN 'Sichuan' WHEN '粤菜' THEN 'Cantonese' WHEN '湘菜' THEN 'Hunan'
        WHEN '江浙菜' THEN 'Jiangsu-Zhejiang' WHEN '东北菜' THEN 'Northeast Chinese' WHEN '鲁菜' THEN 'Shandong'
        WHEN '闽南菜' THEN 'Minnan' WHEN '云南菜' THEN 'Yunnan' WHEN '新疆菜' THEN 'Xinjiang'
        WHEN '轻食' THEN 'Light meals' WHEN '西餐' THEN 'Western' WHEN '日料' THEN 'Japanese'
        WHEN '韩餐' THEN 'Korean' WHEN '东南亚菜' THEN 'Southeast Asian' WHEN '火锅' THEN 'Hot pot'
        WHEN '烧烤' THEN 'Barbecue' WHEN '海鲜' THEN 'Seafood' WHEN '素食' THEN 'Vegetarian'
        WHEN '家常' THEN 'Home-style' WHEN '小吃' THEN 'Street food' WHEN '粉面' THEN 'Noodles'
        WHEN '粥汤' THEN 'Congee and soup' WHEN '快餐' THEN 'Fast food' WHEN '甜品' THEN 'Dessert' ELSE option_value END
    WHEN 'taste' THEN CASE option_value
        WHEN '清淡' THEN 'Mild' WHEN '辣' THEN 'Spicy' WHEN '微辣' THEN 'Slightly spicy'
        WHEN '中辣' THEN 'Medium spicy' WHEN '麻辣' THEN 'Mala' WHEN '甜' THEN 'Sweet'
        WHEN '酸甜' THEN 'Sweet and sour' WHEN '咸鲜' THEN 'Savory' WHEN '鲜香' THEN 'Umami'
        WHEN '酱香' THEN 'Soy sauce' WHEN '蒜香' THEN 'Garlic' WHEN '番茄味' THEN 'Tomato'
        WHEN '咖喱味' THEN 'Curry' WHEN '奶香' THEN 'Creamy' WHEN '油香' THEN 'Rich'
        WHEN '烟火气' THEN 'Smoky' ELSE option_value END
    WHEN 'convenience' THEN CASE option_value
        WHEN '快速' THEN 'Quick' WHEN '慢享' THEN 'Leisurely' WHEN '外带方便' THEN 'Takeaway-friendly'
        WHEN '堂食舒服' THEN 'Comfortable dine-in' WHEN '少排队' THEN 'Short wait'
        WHEN '少餐具' THEN 'Few utensils' WHEN '一人食' THEN 'Solo meal'
        WHEN '多人共享' THEN 'Shared meal' WHEN '适合备餐' THEN 'Meal-prep friendly'
        WHEN '适合边走边吃' THEN 'Easy to eat on the go' ELSE option_value END
    ELSE option_value
END;

UPDATE meal_item
SET name = 'Tomato and egg noodles',
    meal_time = JSON_ARRAY('Lunch', 'Dinner', 'All-day meals'),
    mood = JSON_ARRAY('Tired', 'Low'), scene = JSON_ARRAY('Work', 'Campus', 'Home'),
    health_goal = JSON_ARRAY('Light', 'Gentle on stomach', 'Easy to digest'),
    cuisine = JSON_ARRAY('Home-style', 'Noodles'), taste = JSON_ARRAY('Mild', 'Tomato'),
    convenience = JSON_ARRAY('Quick', 'Solo meal')
WHERE source_type = 'PUBLIC' AND name = '番茄鸡蛋面';

UPDATE meal_item
SET name = 'Clear-broth wonton soup',
    meal_time = JSON_ARRAY('Breakfast', 'Lunch', 'Dinner', 'All-day meals'),
    mood = JSON_ARRAY('Tired', 'No appetite'), scene = JSON_ARRAY('Work', 'Campus', 'Home'),
    health_goal = JSON_ARRAY('Light', 'Gentle on stomach', 'Warm and comforting'),
    cuisine = JSON_ARRAY('Street food', 'Congee and soup'), taste = JSON_ARRAY('Mild', 'Savory'),
    convenience = JSON_ARRAY('Quick', 'Few utensils')
WHERE source_type = 'PUBLIC' AND name = '清汤馄饨';

UPDATE meal_item
SET name = 'Chicken breast grain bowl',
    meal_time = JSON_ARRAY('Lunch', 'Dinner'), mood = JSON_ARRAY('Calm', 'Want to relax'),
    scene = JSON_ARRAY('Work', 'After exercise'),
    health_goal = JSON_ARRAY('Weight loss', 'High protein', 'Low oil', 'Balanced'),
    cuisine = JSON_ARRAY('Light meals'), taste = JSON_ARRAY('Mild', 'Savory'),
    convenience = JSON_ARRAY('Quick', 'Solo meal')
WHERE source_type = 'PUBLIC' AND name = '鸡胸肉轻食碗';

UPDATE meal_item
SET name = 'Spicy dry pot',
    meal_time = JSON_ARRAY('Lunch', 'Dinner', 'Late-night meal'),
    mood = JSON_ARRAY('Happy', 'Want a treat'), scene = JSON_ARRAY('Weekend', 'Group meal', 'Late night'),
    health_goal = JSON_ARRAY('Balanced', 'Energy boost'), cuisine = JSON_ARRAY('Sichuan', 'Street food'),
    taste = JSON_ARRAY('Mala', 'Smoky'), convenience = JSON_ARRAY('Leisurely', 'Shared meal')
WHERE source_type = 'PUBLIC' AND name = '麻辣香锅';

COMMIT;

-- Existing personal meals retain their original tags intentionally. Open and re-save each personal meal
-- using the English tags in the application after running this conversion.

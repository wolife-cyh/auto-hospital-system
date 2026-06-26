/*
 Navicat Premium Dump SQL

 Source Server         : localConnection1
 Source Server Type    : MySQL
 Source Server Version : 90001 (9.0.1)
 Source Host           : localhost:3306
 Source Schema         : smart_medicine

 Target Server Type    : MySQL
 Target Server Version : 90001 (9.0.1)
 File Encoding         : 65001

 Date: 14/06/2026 15:26:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for analysis_history
-- ----------------------------
DROP TABLE IF EXISTS `analysis_history`;
CREATE TABLE `analysis_history`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件名',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/SUCCESS/WARNING/FAILED',
  `progress` int NOT NULL DEFAULT 0 COMMENT '分析进度(0-100)',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `analysis_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分析结果JSON',
  `patient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者姓名',
  `primary_diagnosis` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '主要诊断',
  `confidence` double NULL DEFAULT NULL COMMENT 'AI置信度',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '诊断书分析历史记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of analysis_history
-- ----------------------------

-- ----------------------------
-- Table structure for chat_conversation
-- ----------------------------
DROP TABLE IF EXISTS `chat_conversation`;
CREATE TABLE `chat_conversation`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID(UUID)',
  `user_id` int NOT NULL COMMENT '用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_updated_at`(`updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能医生会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_conversation
-- ----------------------------
INSERT INTO `chat_conversation` VALUES ('ac8e0e48c520483cad61d53a8d503e22', 5, '你是谁', '2026-06-06 02:43:03', '2026-06-06 02:45:32');
INSERT INTO `chat_conversation` VALUES ('de36672099784d9fb5e135a710683840', 5, '简要回答我要吃什么药', '2026-06-06 02:17:15', '2026-06-06 02:17:52');
INSERT INTO `chat_conversation` VALUES ('ef34eb37df3f4919b9a41df70689bda3', 5, '那我应该吃什么', '2026-06-06 02:05:28', '2026-06-06 02:45:58');

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标识',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：user/assistant',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能医生对话历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_history
-- ----------------------------
INSERT INTO `chat_history` VALUES (5, 5, NULL, 'C36760140CFF268AEE0DCE085DDAFA41', 'user', '你是谁', '2026-06-06 01:33:43');
INSERT INTO `chat_history` VALUES (6, 5, NULL, 'C36760140CFF268AEE0DCE085DDAFA41', 'assistant', '您好！我是智慧医疗系统的智能医生助手，专注于为您提供专业、易懂的医疗健康支持。我可以：\n\n✅ 解读体检报告、检验单或影像诊断结果（如血常规、B超、CT报告等）  \n✅ 解释医学术语（比如“窦性心律”“轻度脂肪肝”“尿潜血+”等）  \n✅ 提供科学合理的用药提醒、复查建议和生活方式指导（饮食、运动、作息等）  \n✅ 回答您关于症状、疾病、检查项目等方面的疑问  \n\n⚠️ 温馨提示：我的建议不能替代线下医生的面对面诊疗。如果出现持续不适、剧烈疼痛、高热、出血、意识改变等紧急情况，请立即前往医院急诊科就诊。\n\n请问您目前有具体的报告、症状，或想了解的健康问题吗？我很乐意为您详细解答 🌟', '2026-06-06 01:33:47');
INSERT INTO `chat_history` VALUES (11, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'user', '那我应该吃什么', '2026-06-06 02:05:44');
INSERT INTO `chat_history` VALUES (12, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'assistant', '您好！为了给您提供更准确、安全的饮食建议，我需要先了解一些基本信息，比如：\n\n🔹 您目前是否有正在接受治疗的疾病？（例如：糖尿病、高血压、肾病、胃炎、痛风、术后恢复等）  \n🔹 是否有医生开具的特殊饮食要求？（如低盐、低脂、低嘌呤、低糖、高蛋白、软食等）  \n🔹 最近的检查报告或诊断结果（如血糖、尿酸、肝肾功能、胃肠镜报告等）是否方便简述？  \n🔹 是否存在食物过敏或明显不耐受？（如乳糖不耐、麸质敏感等）  \n🔹 您的主要健康目标是什么？（如控制体重、改善贫血、缓解便秘、增强免疫力、术后康复等）\n\n💡 举个例子：  \n如果您的尿酸偏高（>420 μmol/L），我会建议您避免动物内脏、浓肉汤、海鲜、啤酒，优先选择低嘌呤食物如蛋类、奶类、大部分蔬菜和水果；  \n如果是胃溃疡活动期，则需避免辛辣、过酸、过硬、咖啡/浓茶，推荐温软易消化的流质或半流质饮食。\n\n请您简单告诉我当前情况，我会为您定制一份科学、可执行的饮食建议，并说明原因。您的健康，值得被认真对待 🌟', '2026-06-06 02:05:51');
INSERT INTO `chat_history` VALUES (17, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'user', '简要回答我要吃什么药', '2026-06-06 02:08:01');
INSERT INTO `chat_history` VALUES (18, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'assistant', '抱歉，我无法为您开具或推荐具体药物。\n\n原因如下：  \n🔹 药物使用必须基于明确诊断、个体健康状况（如肝肾功能、过敏史、合并疾病）、药物相互作用及规范剂量——这些都需要面诊医生结合检查结果综合判断；  \n🔹 擅自用药可能存在严重风险（如掩盖病情、加重副作用、引发低血糖/肝损伤/出血等）；  \n🔹 即使是常见药（如退烧药、胃药、降压药），不同成分、剂型、禁忌症也差异很大。\n\n✅ 正确做法：  \n请尽快携带您的诊断报告、近期检查单和正在服用的药物清单，前往正规医院相关科室（如内科、专科门诊）就诊，由医生评估后开具处方。\n\n如果您愿意提供：  \n🔸 具体诊断（如“2型糖尿病”“幽门螺杆菌阳性慢性胃炎”）  \n🔸 主要症状（如“空腹血糖8.5 mmol/L”“上腹胀痛伴反酸”）  \n🔸 已用药物及反应  \n\n我可以帮您：  \n✔ 理解医生开药的目的和注意事项  \n✔ 解释药物常见副作用及应对方法  \n✔ 提醒服药时间、饮食配合要点（如二甲双胍随餐服、奥美拉唑晨起空腹服）  \n\n安全第一，用药务必遵医嘱。需要我帮您解读某份报告或梳理就诊准备事项吗？', '2026-06-06 02:08:08');
INSERT INTO `chat_history` VALUES (19, 5, 'de36672099784d9fb5e135a710683840', NULL, 'user', '简要回答我要吃什么药', '2026-06-06 02:17:32');
INSERT INTO `chat_history` VALUES (20, 5, 'de36672099784d9fb5e135a710683840', NULL, 'assistant', '您好，我无法直接为您开具药物或推荐具体药品，因为用药需基于明确的诊断、个体健康状况（如年龄、基础病、过敏史）、检查结果及医生面诊评估。\n\n请您提供以下信息，我可以帮您更好地理解情况：\n- 当前主要症状（如发热、咳嗽、腹痛等）及持续时间  \n- 是否已就医？是否有诊断报告或检查结果（如血常规、影像报告等）？  \n- 正在服用的其他药物或有无药物过敏史？  \n\n⚠️温馨提示：自行用药可能存在风险，尤其是抗生素、激素类、降压/降糖药等。请务必在医生指导下用药。\n\n如症状紧急（如剧烈胸痛、呼吸困难、意识模糊、严重出血等），请立即前往医院急诊！', '2026-06-06 02:17:36');
INSERT INTO `chat_history` VALUES (21, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'user', '我得了过敏', '2026-06-06 02:17:46');
INSERT INTO `chat_history` VALUES (22, 5, 'de36672099784d9fb5e135a710683840', NULL, 'user', '我得了过敏', '2026-06-06 02:17:52');
INSERT INTO `chat_history` VALUES (23, 5, 'ac8e0e48c520483cad61d53a8d503e22', NULL, 'user', '你是谁', '2026-06-06 02:45:27');
INSERT INTO `chat_history` VALUES (24, 5, 'ac8e0e48c520483cad61d53a8d503e22', NULL, 'assistant', '您好！我是智慧医疗系统的智能医生助手，专注于为您提供专业、易懂的医疗健康支持。我可以：\n\n✅ 解读体检报告、检验单或影像诊断结果（如血常规、B超、CT报告等）  \n✅ 解释医学术语（比如“窦性心律”“轻度脂肪肝”“尿蛋白+”等）  \n✅ 提供常见病的用药提醒、注意事项和生活调理建议（如高血压、糖尿病、感冒、胃炎等）  \n✅ 回答您关于症状、检查、复查时间、药物相互作用等方面的疑问  \n✅ 协助梳理就诊要点，帮您更高效地与医生沟通  \n\n⚠️ 温馨提示：我的建议不能替代面对面诊疗。若出现剧烈疼痛、持续高热、胸闷气促、意识模糊、严重出血等急症表现，请立即拨打120或前往急诊科就医。\n\n请问您目前有具体的检查报告、用药困惑，或想了解哪方面的健康问题？我很乐意为您详细解答 🌟', '2026-06-06 02:45:32');
INSERT INTO `chat_history` VALUES (25, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'user', '我得了过敏啊，为什么不理我', '2026-06-06 02:45:46');
INSERT INTO `chat_history` VALUES (26, 5, 'ef34eb37df3f4919b9a41df70689bda3', NULL, 'assistant', '非常抱歉刚才的回复让您感到被忽视了——您说“我得了过敏”，这非常重要，也完全值得被认真对待！🙏  \n我立刻为您梳理实用、安全的应对建议（但请仍以医生面诊为准）：\n\n✅ **第一步：先稳住症状（若正在发作）**  \n- 如果是轻度：皮肤瘙痒、红疹、打喷嚏、流清涕 → 可临时口服**第二代抗组胺药**（如氯雷他定、西替利嗪），成人常用剂量为每日1片（10mg或10mg），饭后温水送服，避免饮酒和镇静类药物。  \n- 如果出现**嘴唇/喉咙肿胀、呼吸急促、胸闷、头晕或血压下降**——这是**过敏性休克征兆！请立即拨打120或前往急诊，切勿等待！**\n\n✅ **第二步：尽快明确“对什么过敏”**  \n过敏不是病名，而是身体对某种物质（过敏原）的过度反应。常见诱因包括：  \n🔸 食物（鸡蛋、牛奶、花生、海鲜、芒果、小麦等）  \n🔸 吸入物（花粉、尘螨、霉菌、宠物皮屑）  \n🔸 药物（青霉素、阿司匹林、造影剂等）  \n🔸 昆虫叮咬（蜂毒、蚊虫）  \n👉 建议尽快到医院**变态反应科或皮肤科**做过敏原检测（如抽血IgE检测或皮肤点刺试验），找到元凶才能真正预防复发。\n\n✅ **第三步：今天就能做的自我保护**  \n- 记录「症状发生时间+地点+吃了/接触了什么+持续多久」→ 帮助医生快速判断线索；  \n- 暂停食用新尝试的食物、停用新换的护肤品/洗发水/药物；  \n- 保持环境清洁（勤换床单、用防螨床罩、减少毛绒玩具）；  \n- 避免搔抓皮疹，可冷敷缓解瘙痒，勿用热水烫洗。\n\n需要我帮您：  \n🔹 列一份「常见过敏原自查清单」？  \n🔹 解释过敏原检测报告怎么看？  \n🔹 教您写一份清晰的「过敏就诊自述模板」（节省医生问诊时间）？  \n\n您现在的症状是？（比如：身上起风团？眼睛肿？咳嗽喘？还是吃东西后嘴麻？）  \n我在这里，认真听您说 👂', '2026-06-06 02:45:58');

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '反馈用户',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱地址',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '反馈标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '反馈内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES (6, '路人甲', '31952874@qq.com', '测试一号', '测试这个系统有问题吗？', '2022-05-03 16:13:59', '2022-05-03 16:13:59');
INSERT INTO `feedback` VALUES (7, '路人乙', '31952874@qq.com', '测试二号', '惆怅长岑长错错错错错错', '2022-05-03 16:14:20', '2022-05-03 16:14:20');

-- ----------------------------
-- Table structure for history
-- ----------------------------
DROP TABLE IF EXISTS `history`;
CREATE TABLE `history`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户搜索历史主键id',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `keyword` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '搜索关键字',
  `operate_type` int NULL DEFAULT NULL COMMENT '类型：1搜索，2科目，3药品',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 160 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of history
-- ----------------------------
INSERT INTO `history` VALUES (126, 4, '10,无', 1, '2022-05-03 16:09:34', '2022-05-03 16:09:34');
INSERT INTO `history` VALUES (127, 4, '10,无', 1, '2022-05-03 16:09:40', '2022-05-03 16:09:40');
INSERT INTO `history` VALUES (128, 4, '病毒性感冒', 2, '2022-05-03 16:09:48', '2022-05-03 16:09:48');
INSERT INTO `history` VALUES (129, 4, '10,无', 1, '2022-05-03 16:09:52', '2022-05-03 16:09:52');
INSERT INTO `history` VALUES (130, 4, '湿疹', 2, '2022-05-03 16:12:15', '2022-05-03 16:12:15');
INSERT INTO `history` VALUES (131, 4, '偏头痛', 2, '2022-05-03 16:12:49', '2022-05-03 16:12:49');
INSERT INTO `history` VALUES (132, 5, '7,无', 1, '2022-05-03 16:17:41', '2022-05-03 16:17:41');
INSERT INTO `history` VALUES (133, 5, '湿疹', 2, '2022-05-03 16:17:53', '2022-05-03 16:17:53');
INSERT INTO `history` VALUES (134, 5, '感冒', 2, '2022-05-03 16:18:08', '2022-05-03 16:18:08');
INSERT INTO `history` VALUES (135, 5, '17,无', 1, '2022-05-03 16:18:22', '2022-05-03 16:18:22');
INSERT INTO `history` VALUES (136, 5, '17,溃疡', 1, '2022-05-03 16:18:28', '2022-05-03 16:18:28');
INSERT INTO `history` VALUES (137, 5, '溃疡', 2, '2022-05-03 16:18:28', '2022-05-03 16:18:28');
INSERT INTO `history` VALUES (138, 5, '17,溃疡', 1, '2022-05-03 16:26:48', '2022-05-03 16:26:48');
INSERT INTO `history` VALUES (139, 5, '溃疡', 2, '2022-05-03 16:26:48', '2022-05-03 16:26:48');
INSERT INTO `history` VALUES (140, 5, '17,溃疡', 1, '2022-05-03 16:28:20', '2022-05-03 16:28:20');
INSERT INTO `history` VALUES (141, 5, '溃疡', 2, '2022-05-03 16:28:20', '2022-05-03 16:28:20');
INSERT INTO `history` VALUES (142, 5, '17,溃疡', 1, '2022-05-03 16:33:52', '2022-05-03 16:33:52');
INSERT INTO `history` VALUES (143, 5, '溃疡', 2, '2022-05-03 16:33:52', '2022-05-03 16:33:52');
INSERT INTO `history` VALUES (144, 5, '溃疡', 2, '2022-05-03 16:34:08', '2022-05-03 16:34:08');
INSERT INTO `history` VALUES (145, 5, '7,无', 1, '2022-05-03 16:37:57', '2022-05-03 16:37:57');
INSERT INTO `history` VALUES (146, 5, '9,无', 1, '2022-05-03 16:38:34', '2022-05-03 16:38:34');
INSERT INTO `history` VALUES (147, 5, '9,无', 1, '2022-05-03 16:41:59', '2022-05-03 16:41:59');
INSERT INTO `history` VALUES (148, 5, '9,无', 1, '2022-05-03 16:42:14', '2022-05-03 16:42:14');
INSERT INTO `history` VALUES (149, 5, '9,无', 1, '2022-05-03 16:42:45', '2022-05-03 16:42:45');
INSERT INTO `history` VALUES (150, 5, '9,无', 1, '2022-05-03 16:43:54', '2022-05-03 16:43:54');
INSERT INTO `history` VALUES (151, 5, '9,无', 1, '2022-05-03 16:44:26', '2022-05-03 16:44:26');
INSERT INTO `history` VALUES (152, 5, '9,无', 1, '2022-05-03 16:44:45', '2022-05-03 16:44:45');
INSERT INTO `history` VALUES (153, 5, '2,无', 1, '2022-05-03 16:44:51', '2022-05-03 16:44:51');
INSERT INTO `history` VALUES (154, 5, '2,无', 1, '2022-05-03 16:45:46', '2022-05-03 16:45:46');
INSERT INTO `history` VALUES (155, 5, '1', 3, '2022-05-07 15:34:34', '2022-05-07 15:34:34');
INSERT INTO `history` VALUES (156, 4, '牙周炎', 2, '2022-07-14 19:32:05', '2022-07-14 19:32:05');
INSERT INTO `history` VALUES (157, 4, '9,无', 1, '2022-07-14 19:32:52', '2022-07-14 19:32:52');
INSERT INTO `history` VALUES (158, 4, '1,无', 1, '2022-07-14 19:32:56', '2022-07-14 19:32:56');
INSERT INTO `history` VALUES (159, 4, '17,无', 1, '2022-07-14 19:32:59', '2022-07-14 19:32:59');

-- ----------------------------
-- Table structure for illness
-- ----------------------------
DROP TABLE IF EXISTS `illness`;
CREATE TABLE `illness`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '疾病id',
  `kind_id` int NULL DEFAULT NULL COMMENT '疾病分类ID',
  `illness_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '疾病名字',
  `include_reason` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '诱发因素',
  `illness_symptom` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '疾病症状',
  `special_symptom` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '特殊症状',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of illness
-- ----------------------------
INSERT INTO `illness` VALUES (1, 10, '病毒性感冒', '各种导致全身或呼吸道局部防御功能降低的原因，如受凉、淋雨、气候突变、过度疲劳等均可诱发本疲', '急性起病，患者主要表现为鼻塞、流涕、咽痛、咳嗽等症状。部分患者可有发热、乏力、头痛、周身酸痛、\n食欲减退、腹胀、便秘或腹泻等全身症状。', '急性起病，患者主要表现为鼻塞、流涕、咽痛、咳嗽等症状。部分患者可有发热、乏力、头痛、周身酸痛、\n食欲减退、腹胀、便秘或腹泻等全身症状。', '2022-05-01 11:31:10', '2022-05-03 16:00:41');
INSERT INTO `illness` VALUES (2, 10, '风寒感冒', '身体免疫力低下的情况下，体内有寒气入侵，导致感冒', '恶寒重、发热轻、无汗、头痛身痛、鼻塞流清涕、咳嗽吐稀白痰、口不渴或渴喜热饮、苔薄白', '恶寒重、发热轻、无汗、头痛身痛、畏寒', '2022-05-01 11:31:10', '2022-05-03 16:01:51');
INSERT INTO `illness` VALUES (3, 2, '扁桃体发炎', '扁桃体炎形成的原因与多种因素有关，包括感染因素、免疫因素、邻近器官的急性炎症等，细菌和病毒积存\n于扁桃体窝引起该病。扁桃体炎还可继发于某些急性传染病，如猩红热、麻疹、流感等。', '发热、咽部不适、咽部疼痛，甚至吞咽、呼吸困难、局部可有咽痛，吞咽时尤为明显，甚至因畏惧疼痛不敢吞咽，疼痛可放射至耳部，幼儿常因不能吞咽而拒食哭闹。', '咽痛、咽部不适', '2022-05-01 11:31:10', '2022-05-01 17:49:05');
INSERT INTO `illness` VALUES (4, 3, '偏头痛', '偏头痛的病因尚不明确，可能与遗传、内分泌代谢、环境因素、精神因素有关。', '偏头痛的常见症状包括：头痛，开始常为隐约疼痛，逐渐变为搏动性疼痛，活动时加重，还可从头的一侧转\n移至另一侧，累及头前部或整个头部；对光线、噪音和气味敏感；伴有恶心、呕吐，胃部不适，腹部疼痛；\n食欲差；感觉非常的暖或冷；肤色苍白；疲劳；头晕；视野模糊；腹泻。比较军见的症状包括发烧、影响正\n常的肢体活动。', '左侧疼痛、右侧疼痛、单侧疼痛、一阵一阵的疼痛、像针扎一样', '2022-05-01 11:31:10', '2022-05-01 17:49:06');
INSERT INTO `illness` VALUES (5, 2, '便秘', '便秘通常是由于美便在消化道中移动太慢，或无法从直肠中有效清除时，导致类便脱水、变硬和干燥，从而引发的便秘。', '排便次数减少、一周内小于3次、粪便干燥或结块、如羊粪、排便因难，如排便时间长、排便时感觉有阻碍、排便后仍有粪便未排尽的感觉、需手按腹部帮助排便等', '大便困难、拉不出来', '2022-05-01 11:31:10', '2022-05-03 16:03:44');
INSERT INTO `illness` VALUES (6, 3, '骨折', '骨折是由创伤或骨骼痪病所导致，大部分骨折都是由于直接或间接暴力引起。跌倒、撞击、交通意外等暴力因素是导致骨折的常见原因。积累性劳损及骨骼痪病也会增加骨折发生几率，骨骼痪病（如骨髓炎、骨肿瘤）导致骨质破坏，患者受到轻微外力就可能发生骨折。', '骨折特有特征为畸形、异常活动和骨擦音（感）。大部分骨折一般只引起局部症状，最常见的症状就是局部\n疼痛、肿胀及功能障碍。严重骨折和多发性骨折可伴随全身症状（如休克、发热）。', '骨折的一般表现为局部疼痛、肿胀及功能障碍', '2022-05-01 11:31:10', '2022-05-03 16:04:23');
INSERT INTO `illness` VALUES (7, 17, '牙周炎', '牙周炎是一种破坏性庆病，与微生物、宿主反应有关，是导致我国成人牙齿丧失的主要原因，严重影响患者的口腔健康。在局部致病因素中，牙菌斑是最主要的致病因素，而在全身因素中吸烟是高危因素。', '健康的牙龈应该呈粉红色，边缘薄且紧贴牙面，质坚韧，探诊不出血。牙周災的主要症状是牙龈红肿、质地\n松软、探诊出血、牙周袋溢脓和牙齿松动。', '牙龈出血、牙齿松动、牙龈肿', '2022-05-01 11:31:10', '2022-05-03 16:04:43');
INSERT INTO `illness` VALUES (8, 2, '胃溃疡', '胃溃疡是一种常见的消化痪病，任何年龄的人都可能患病。在全球范围内，约占10%的人群一生中都会患有消化性渍疡。在患病人群中，40-60岁的中老年患者最为多见，而且男性多于女性。', '胃溃疡的症状较多，包括胃部疼痛、食欲不振、餐后腹胀或胃部不适、体重减轻等等。这些症状的严重程度\n取决于溃疡的严重程度。有些患者可能没有任何症状（如“无症状性溃疡\"），或者是以胃出血、胃穿孔等并\n发症为首发症状。', '餐后腹胀、体重减轻、食欲不振', '2022-05-01 11:31:10', '2022-05-03 16:05:16');
INSERT INTO `illness` VALUES (9, 17, '口腔溃疡', '口腔渍疡的致病原因尚不明确，多种因素可诱发，包括遗传因素、饮食因素、免疫因素等，且具有明显的个体差异。口腔渍疡经常、反复发作时，严重影响患者的日常生活和工作。', '口腔溃疡常见于口腔的唇、脸颊、软腭或牙龈等处的黏膜上，溃疡面一般呈圆形或椭圆形，溃疡面凹陷、有\n白色或黄色的中心、周围充血微红肿，有明显疼痛感。', '口腔溃疡常见于口腔的唇、脸颊、软腭或牙龈等处的黏膜上，溃疡面一般呈圆形或椭圆形，溃疡面凹陷、有\n白色或黄色的中心、周围充血微红肿，有明显疼痛感。', '2022-05-01 11:31:10', '2022-05-03 16:05:51');
INSERT INTO `illness` VALUES (13, 7, '湿疹', '湿疹的病因目前尚不明确，与机体内因、外因、社会心理因素等都有关。机体内因包括免疫功能异常和系统性痪病（如内分泌痪病、营养障碍、慢性感染等）以及遗传性或获得性 皮肤屏障功能障碍。', '急性期表现为红斑、水肿、粟粒大小的丘疹、丘疱疹、水疱，糜烂及渗出；亚急性期表现为红肿和渗出减\n轻，糜烂面结痂、脱屑；慢性期主要表现为粗糙肥厚、苔藓样变。湿疹容易复发，严重影响患者的生活质\n量。', '起病较急、发病较快，瘙痒剧烈。', '2022-05-03 16:08:58', '2022-05-03 16:09:11');

-- ----------------------------
-- Table structure for illness_kind
-- ----------------------------
DROP TABLE IF EXISTS `illness_kind`;
CREATE TABLE `illness_kind`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类名称',
  `info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of illness_kind
-- ----------------------------
INSERT INTO `illness_kind` VALUES (1, '急诊科', '急诊科疾病', '2022-05-01 11:57:39', '2022-05-01 12:01:00');
INSERT INTO `illness_kind` VALUES (2, '内科', '内科疾病', '2022-05-01 11:57:57', '2022-05-01 12:00:59');
INSERT INTO `illness_kind` VALUES (3, '外科', '外科疾病', '2022-05-01 11:58:26', '2022-05-01 12:00:57');
INSERT INTO `illness_kind` VALUES (4, '妇产科', '妇产科疾病', '2022-05-01 11:58:36', '2022-05-01 12:00:56');
INSERT INTO `illness_kind` VALUES (5, '儿科', '儿科疾病', '2022-05-01 11:58:49', '2022-05-01 12:00:54');
INSERT INTO `illness_kind` VALUES (6, '男科', '男科疾病', '2022-05-01 11:58:59', '2022-05-01 12:00:53');
INSERT INTO `illness_kind` VALUES (7, '皮肤科', '皮肤科疾病', '2022-05-03 16:07:12', '2022-05-03 16:07:12');
INSERT INTO `illness_kind` VALUES (9, '肝病', '肝病疾病', '2022-05-01 11:59:27', '2022-05-01 12:00:49');
INSERT INTO `illness_kind` VALUES (10, '传染科', '传染科疾病', '2022-05-01 11:59:35', '2022-05-01 12:00:48');
INSERT INTO `illness_kind` VALUES (16, '耳鼻喉科', '耳鼻喉科疾病', '2022-05-01 12:00:23', '2022-05-01 12:00:41');
INSERT INTO `illness_kind` VALUES (17, '口腔科', '口腔科疾病', '2022-05-01 12:00:31', '2022-05-01 12:00:39');

-- ----------------------------
-- Table structure for illness_medicine
-- ----------------------------
DROP TABLE IF EXISTS `illness_medicine`;
CREATE TABLE `illness_medicine`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '病和药品关联id',
  `illness_id` int NULL DEFAULT NULL COMMENT '病id',
  `medicine_id` int NULL DEFAULT NULL COMMENT '药品id',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of illness_medicine
-- ----------------------------
INSERT INTO `illness_medicine` VALUES (6, 3, 1, '2022-05-03 16:10:35', '2022-05-03 16:10:35');
INSERT INTO `illness_medicine` VALUES (7, 2, 1, '2022-05-03 16:10:37', '2022-05-03 16:10:37');
INSERT INTO `illness_medicine` VALUES (8, 1, 1, '2022-05-03 16:10:38', '2022-05-03 16:10:38');
INSERT INTO `illness_medicine` VALUES (9, 4, 1, '2022-05-03 16:10:42', '2022-05-03 16:10:42');
INSERT INTO `illness_medicine` VALUES (10, 7, 1, '2022-05-03 16:10:44', '2022-05-03 16:10:44');
INSERT INTO `illness_medicine` VALUES (11, 1, 2, '2022-05-03 16:10:59', '2022-05-03 16:10:59');
INSERT INTO `illness_medicine` VALUES (12, 2, 2, '2022-05-03 16:11:01', '2022-05-03 16:11:01');
INSERT INTO `illness_medicine` VALUES (13, 5, 3, '2022-05-03 16:11:16', '2022-05-03 16:11:16');
INSERT INTO `illness_medicine` VALUES (14, 13, 5, '2022-05-03 16:11:29', '2022-05-03 16:11:29');
INSERT INTO `illness_medicine` VALUES (15, 8, 4, '2022-05-03 16:11:39', '2022-05-03 16:11:39');
INSERT INTO `illness_medicine` VALUES (16, 7, 6, '2022-05-03 16:11:50', '2022-05-03 16:11:50');
INSERT INTO `illness_medicine` VALUES (17, 4, 7, '2022-05-03 16:12:01', '2022-05-03 16:12:01');
INSERT INTO `illness_medicine` VALUES (18, 2, 7, '2022-05-03 16:12:03', '2022-05-03 16:12:03');
INSERT INTO `illness_medicine` VALUES (19, 1, 7, '2022-05-03 16:12:04', '2022-05-03 16:12:04');
INSERT INTO `illness_medicine` VALUES (20, 3, 7, '2022-05-03 16:12:05', '2022-05-03 16:12:05');

-- ----------------------------
-- Table structure for medicine
-- ----------------------------
DROP TABLE IF EXISTS `medicine`;
CREATE TABLE `medicine`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '药品主键ID',
  `medicine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药的名字',
  `keyword` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关键字搜索',
  `medicine_effect` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '药的功效',
  `medicine_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药的品牌',
  `interaction` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '药的相互作用',
  `taboo` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '禁忌',
  `us_age` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用法用量',
  `medicine_type` int NULL DEFAULT NULL COMMENT '药的类型，0西药，1中药，2中成药',
  `img_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关图片路径',
  `medicine_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '药的价格',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medicine
-- ----------------------------
INSERT INTO `medicine` VALUES (1, '阿莫西林胶囊', '消炎药、感冒药', '本品尚可用于治疗伤寒、伤寒带菌者及钩端螺旋体病；阿莫西林亦可与克拉霉素、兰索拉唑三联用药根除胃、十二指肠幽门螺杆菌，降低消化道溃疡复发率。', '仁和', '1．丙磺舒竞争性地减少本品的肾小管分泌，两者同时应用可引起阿莫西林血浓度升高、半衰期延长。\n2．氯霉素、大环内酯类、磺胺类和四环素类药物在体外干扰阿莫西林的抗菌作用，但其临床意义不明。', '1. 青霉素类口服药可引起过敏性休克，有多见于青霉素或头孢菌素过敏史的患者。用药前必须详细询问药物过敏史并做青霉素皮肤试验。如发生过敏性休克，应就地抢救，予以保持气道畅通、吸氧及应用肾上腺素、糖皮质激素等治疗措施。\n2.传染性单核细胞增多症患者应用本品易发生皮疹，应避免使用。\n3.疗程较长患者应检查肝、肾功能和血常规。\n4.阿莫西林可导致采用Benedict或Fehling试剂的尿糖试验出现假阳性。\n5.下列情况应慎用：\n(1)有哮喘、枯草热等过敏性疾病史者。\n(2)老年人和肾功能严重损害时可能须调整剂量。', '成人的具体使用剂量为0.5g/次，每6-8小时重复用药，24小时内服用剂量不能超过4g。儿童一日用药剂量按照患者实际体重为20-40mg/kg，重复用药间隔时长为8h/次。', 1, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 14.00, '2022-05-02 11:46:00', '2022-05-03 16:10:46');
INSERT INTO `medicine` VALUES (2, '999感冒灵颗粒', '感冒药、流鼻涕、发烧', '解热镇痛功效，用于因感冒引起的头痛，发热，鼻塞，流涕，咽痛等症状。', '999', '三九感冒灵颗粒是复方药，里面含有中西药成分，不宜和西药感冒药同服。如果两种药中含有同一种成分，就只能选择服用其中一种，以免使摄入药量加倍，增加毒性，成为重复用药 [3]  。比如，三九感冒灵颗粒和西药泰诺，都含有解热镇痛效果的“扑热息痛”成分，若是两种药一起吃，过量的“扑热息痛”会对人体肝脏造成损害。', '1.忌烟，酒及辛辣，生冷，油腻食物。\n2.不宜在服药期间同时服用滋补性中成药\n3. 高血压、心脏病、肝病、肾病等慢性病严重者应在医师指导下服用。\n4.本品含对乙氨基酚，马来酸氨苯那敏，咖啡因。服用本品期间不得饮酒或含有酒精的饮料；不能同时服用与本品成分相似的其它抗感冒药；肝，肾功能不全者慎用；膀胱颈梗阻，甲状腺功能亢进，青光眼，高血压和前列腺肥大者慎用；孕妇及哺乳期妇女慎用；服药期间不得驾驶机，车，船，从事高空作业，机械作业及操作精密仪器。\n5.脾胃虚寒，症见腹痛，喜暖，泄泻者慎用。\n6.糖尿病患者、消化道溃疡患者、膀胱颈梗阻、幽门十二指肠梗阻、甲状腺机能亢进、青光眼以及前列腺肥大等患者慎用。\n7.儿童，年老体弱者应在医师指导下使用。\n8.服药3天后症状无改善，或症状加重，或出现新的严重症状如胸闷，心悸等应立即停药，并去医院就诊。\n9.对本药过敏者禁用，过敏体质者慎用。\n10. 本品性状发生改变时禁止使用。\n11.儿童必须在成人监护下使用。\n12.请将本品放在儿童不能接触的地方。\n13.如正在使用其他药品，使用本品前请咨询医师或药师.', '开水冲服，一次1袋，一日3次。小儿减量或遵医嘱。', 2, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/5de3c4ca4b0346279a2c01e28b323641.jpeg', 39.80, '2022-05-02 11:50:13', '2022-05-03 12:47:16');
INSERT INTO `medicine` VALUES (3, '开塞露', '便秘', '都是利用甘油或山梨醇的高浓度，即高渗作用，软化大便，刺激肠壁，反射性地引起排便反应，再加上其具有润滑作用，能使大便容易排出', '信龙', NULL, '1.刺破或剪开后的注药导管的开口应光滑，以免擦伤肛门或直肠。\n2.对本品过敏者禁用，过敏体质者慎用。\n3.本品性状发生改变时禁止使用。\n4.请将本品放在儿童不能接触的地方。\n5.儿童必须在成人监护下使用。\n6.如正在使用其他药品，使用本品前请咨询医师或药师。', '将容器顶端刺破或剪开，涂以油脂少许，缓慢插入肛门，然后将药液挤入直肠内，成人一次1支，儿童一次\n0.5支。', 0, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/71bdf290e1ca4ebe94a87fdc6d1ee26e.jpeg', 18.00, '2022-05-02 12:52:13', '2022-05-03 12:49:31');
INSERT INTO `medicine` VALUES (4, '三九胃泰颗粒', '胃胀、胃痛、胃不舒服', '清热祛湿，消炎止痛，理气除胀，养胃益肠。', '999', NULL, '1． 服药期间，忌食辛辣，油炸，过酸食物及酒类等刺激性食品。\n2． 十五天为一疗程，初显疗效后不宜立即停药，建议再服3—4个疗程以巩固疗效。\n3．胃寒患者慎用。', '开水冲服。一次1袋，一日2次。', 0, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/e2c6781609d942a0937028e1e7f84097.jpeg', 15.00, '2022-05-02 12:58:32', '2022-05-03 12:51:49');
INSERT INTO `medicine` VALUES (5, '999皮炎平', '皮肤瘙痒', '用于局限性瘙痒症、神经性皮炎、接触性皮炎、脂溢性皮炎以及慢性湿疹。', '999', NULL, '1.患处已破溃、化脓或有明显渗出者禁用。\n2.病毒感染者（如有疱疹、水痘）禁用。\n3.对本品成分过敏者禁用。', '皮肤外用。取少量涂于患处，并轻揉片刻；一日1~2次，病情较重或慢性炎症患者，每日5-8次或遵医嘱。', 0, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/163549185ab24e068e59ba6ef502c410.jpeg', 15.21, '2022-05-02 13:01:34', '2022-05-03 12:52:46');
INSERT INTO `medicine` VALUES (6, '甲硝唑', '牙痛', '适应症为用于治疗肠道和肠外阿米巴病（如阿米巴肝脓肿、胸膜阿米巴病等）。还可用于治疗阴道滴虫病、小袋虫病和皮肤利什曼病、麦地那龙线虫感染等。目前还广泛用于厌氧菌感染的治疗', '奥可安', '本品能增强华法林等抗凝药物的作用。与土霉素合用可干扰甲硝唑清除阴道滴虫的作用。', '有活动性中枢神经系统疾患和血液病者禁用。', '成人一次两片，一日三次', 0, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/4c6b2d32ef9f43b59ee1268370da2b98.jpeg', 28.50, '2022-05-02 13:03:27', '2022-05-03 12:54:01');
INSERT INTO `medicine` VALUES (7, '布洛芬缓释胶囊', '头疼、缓解痛', '用于缓解轻至中度疼痛如头痛、偏头痛、牙痛、痛经、关节痛、肌肉痛、神经痛，也用于普通感冒或流行性感冒引起的发热', '芬必得', '.本品与其他解热、镇痛、抗炎药物同用时可增加胃肠道不良反应，并可能导致溃疡。 2.本品与肝素、双香豆素类(如华法林)等抗凝药 同用时，可导致凝血酶原时间延长，增加出血倾向。 3.本品与地高辛、甲氨蝶呤、口服降血糖药物同用 时，能使这些药物的血药浓度增高，不宜同用。 ', '1.对其他非甾休抗炎药过敏者禁用。 2.孕妇及晡乳期妇女禁用。 3.对阿司匹林过敏的哮喘患者禁用。 4.严重肝肾功能不全者或严重心力衰竭者禁用。 5.正在服用其他含有布洛芬或其他非甾休抗炎药， 包括服用已知是特异性环氧化酶-2抑制剂药物的患者禁用。除非医生建议使用。 6.既往有与使用非甾体类抗炎药治疗相关的上消化道出血或穿孔史者禁用。 7.活动性或既往有消化性溃疡史，胃肠道出血或穿孔的患者禁用。', '口服。成人，一次1片，一日2次（早晚各一次）。', 1, 'https://su-share.oss-cn-beijing.aliyuncs.com/3/92af48e2bd85458bb4f46ea731a31b58.jpeg', 1.00, '2022-05-02 13:10:47', '2022-05-03 15:05:30');

-- ----------------------------
-- Table structure for pageview
-- ----------------------------
DROP TABLE IF EXISTS `pageview`;
CREATE TABLE `pageview`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `pageviews` int NULL DEFAULT NULL COMMENT '浏览量',
  `illness_id` int NULL DEFAULT NULL COMMENT '病的id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pageview
-- ----------------------------
INSERT INTO `pageview` VALUES (5, 5, 1);
INSERT INTO `pageview` VALUES (6, 4, 13);
INSERT INTO `pageview` VALUES (7, 2, 4);
INSERT INTO `pageview` VALUES (8, 1, 2);
INSERT INTO `pageview` VALUES (9, 1, 3);
INSERT INTO `pageview` VALUES (10, 1, 5);
INSERT INTO `pageview` VALUES (11, 1, 6);
INSERT INTO `pageview` VALUES (12, 2, 7);
INSERT INTO `pageview` VALUES (13, 1, 8);
INSERT INTO `pageview` VALUES (14, 1, 9);

-- ----------------------------
-- Table structure for purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_id` int NOT NULL,
  `medicine_id` int NOT NULL,
  `medicine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `medicine_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `price` decimal(10, 2) NULL DEFAULT NULL,
  `total_price` decimal(10, 2) NOT NULL,
  `status` int NOT NULL DEFAULT 0,
  `pay_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pay_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_medicine_id`(`medicine_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品购买订单\r\n' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of purchase_order
-- ----------------------------
INSERT INTO `purchase_order` VALUES (1, 'MED202605301446299830', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-05-30 14:46:45', '2026-05-30 14:46:29', '2026-05-30 14:46:29');
INSERT INTO `purchase_order` VALUES (2, 'MED202605301451372303', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 3, 14.00, 42.00, 2, 'alipay', NULL, '2026-05-30 14:51:37', '2026-05-30 14:51:37');
INSERT INTO `purchase_order` VALUES (3, 'MED202605301451454211', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 3, 14.00, 42.00, 1, 'mock', '2026-05-30 14:51:47', '2026-05-30 14:51:45', '2026-05-30 14:51:45');
INSERT INTO `purchase_order` VALUES (4, 'MED202605301456307100', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 2, 'alipay', NULL, '2026-05-30 14:56:30', '2026-05-30 14:56:30');
INSERT INTO `purchase_order` VALUES (5, 'MED202605301456353202', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-05-30 14:56:37', '2026-05-30 14:56:35', '2026-05-30 14:56:35');
INSERT INTO `purchase_order` VALUES (6, 'MED202605301459526401', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-05-30 15:00:11', '2026-05-30 14:59:52', '2026-05-30 14:59:52');
INSERT INTO `purchase_order` VALUES (7, 'MED202605301500248524', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 2, NULL, NULL, '2026-05-30 15:00:24', '2026-05-30 15:00:24');
INSERT INTO `purchase_order` VALUES (8, 'MED202605301500533635', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-05-30 15:01:01', '2026-05-30 15:00:53', '2026-05-30 15:00:53');
INSERT INTO `purchase_order` VALUES (9, 'MED202605301504154325', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 2, NULL, NULL, '2026-05-30 15:04:15', '2026-05-30 15:04:15');
INSERT INTO `purchase_order` VALUES (10, 'MED202605301504534530', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 2, NULL, NULL, '2026-05-30 15:04:53', '2026-05-30 15:04:53');
INSERT INTO `purchase_order` VALUES (11, 'MED202605301506417250', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 0, NULL, NULL, '2026-05-30 15:06:41', '2026-05-30 15:06:41');
INSERT INTO `purchase_order` VALUES (12, 'MED202605301506491348', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 0, NULL, NULL, '2026-05-30 15:06:49', '2026-05-30 15:06:49');
INSERT INTO `purchase_order` VALUES (13, 'MED202605301544539074', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-05-30 15:44:56', '2026-05-30 15:44:53', '2026-05-30 15:44:53');
INSERT INTO `purchase_order` VALUES (14, 'MED202605301721346965', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-05-30 17:21:38', '2026-05-30 17:21:34', '2026-05-30 17:21:34');
INSERT INTO `purchase_order` VALUES (15, 'MED202606071443529737', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 0, 'alipay', NULL, '2026-06-07 14:43:52', '2026-06-07 14:43:52');
INSERT INTO `purchase_order` VALUES (16, 'MED202606071447122334', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 0, 'alipay', NULL, '2026-06-07 14:47:12', '2026-06-07 14:47:12');
INSERT INTO `purchase_order` VALUES (17, 'MED202606071450154410', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 0, 'alipay', NULL, '2026-06-07 14:50:15', '2026-06-07 14:50:15');
INSERT INTO `purchase_order` VALUES (18, 'MED202606071500299628', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-06-07 15:15:18', '2026-06-07 15:00:29', '2026-06-07 15:00:29');
INSERT INTO `purchase_order` VALUES (19, 'MED202606071509461883', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-06-07 15:12:48', '2026-06-07 15:09:46', '2026-06-07 15:09:46');
INSERT INTO `purchase_order` VALUES (20, 'MED202606071513585244', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 1, 'mock', '2026-06-07 15:15:08', '2026-06-07 15:13:58', '2026-06-07 15:13:58');
INSERT INTO `purchase_order` VALUES (21, 'MED202606071516483949', 4, 5, '999皮炎平', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/163549185ab24e068e59ba6ef502c410.jpeg', 1, 15.21, 15.21, 1, 'mock', '2026-06-07 15:17:23', '2026-06-07 15:16:48', '2026-06-07 15:16:48');
INSERT INTO `purchase_order` VALUES (22, 'MED202606071523395909', 4, 1, '阿莫西林胶囊', 'https://su-share.oss-cn-beijing.aliyuncs.com/3/be347ae15e0a467da83e032e8f731535.jpeg', 1, 14.00, 14.00, 0, 'alipay', NULL, '2026-06-07 15:23:39', '2026-06-07 15:23:39');
INSERT INTO `purchase_order` VALUES (23, 'MED202606071627407233', 4, 1, '阿莫西林胶囊', '/assets/images/medicine/amoxicillin.jpg', 1, 14.00, 14.00, 1, 'mock', '2026-06-07 16:27:44', '2026-06-07 16:27:40', '2026-06-07 16:27:40');
INSERT INTO `purchase_order` VALUES (24, 'MED202606071628211521', 4, 2, '999感冒灵颗粒', '/assets/images/medicine/ganmaoling.webp', 1, 39.80, 39.80, 1, 'mock', '2026-06-07 16:28:24', '2026-06-07 16:28:21', '2026-06-07 16:28:21');
INSERT INTO `purchase_order` VALUES (25, 'MED202606071628401207', 4, 3, '开塞露', '/assets/images/medicine/kasailu.jpg', 1, 18.00, 18.00, 1, 'mock', '2026-06-07 16:28:48', '2026-06-07 16:28:40', '2026-06-07 16:28:40');
INSERT INTO `purchase_order` VALUES (26, 'MED202606091818362959', 4, 1, '阿莫西林胶囊', '/assets/images/medicine/amoxicillin.jpg', 1, 14.00, 14.00, 1, 'mock', '2026-06-09 18:18:51', '2026-06-09 18:18:36', '2026-06-09 18:18:36');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户主键id',
  `user_account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户账号',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户的真实名字',
  `user_pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户密码',
  `user_age` int NULL DEFAULT NULL COMMENT '用户年龄',
  `user_sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户性别',
  `user_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `user_tel` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `role_status` int NULL DEFAULT NULL COMMENT '角色状态，1管理员，0普通用户',
  `img_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (4, 'admin', '管理员', '123456', 23, '男', '2678788262@qq.com', '17746678954', 1, 'https://moti-cloud-v2.oss-cn-beijing.aliyuncs.com/Snipaste_2022-05-01_15-37-01.png', '2022-05-03 15:55:41', '2022-05-03 15:56:15');
INSERT INTO `user` VALUES (5, 'zhangsan', '张三', '123456', 23, '女', 'test@example.com', '17879544343', 0, 'https://su-share.oss-cn-beijing.aliyuncs.com/5/5dc107dcd2db4cbd8ad561f4c1642886.png', '2022-05-03 16:15:53', '2022-05-03 16:17:12');

SET FOREIGN_KEY_CHECKS = 1;

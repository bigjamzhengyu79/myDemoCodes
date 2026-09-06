-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: aiproject
-- ------------------------------------------------------
-- Server version	8.0.45
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `assignment_questions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignment_questions` (
  `assignment_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  KEY `FK65v3i3s7uk5i9mumieswarg6e` (`question_id`),
  KEY `FKhfepd59o3qem909sjpa4fblra` (`assignment_id`),
  CONSTRAINT `FK65v3i3s7uk5i9mumieswarg6e` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `FKhfepd59o3qem909sjpa4fblra` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignments` (
  `created_at` datetime(6) DEFAULT NULL,
  `due_time` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_time` datetime(6) DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  `class_name` varchar(64) DEFAULT NULL,
  `description` text,
  `title` varchar(255) NOT NULL,
  `status` enum('DRAFT','PUBLISHED','CLOSED') NOT NULL,
  `class_group_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK67msc7a52b0l2pttoq5bhm6bk` (`teacher_id`),
  KEY `FKrne5jeeuacssvyxeqd9mddh1i` (`class_group_id`),
  CONSTRAINT `FK67msc7a52b0l2pttoq5bhm6bk` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrne5jeeuacssvyxeqd9mddh1i` FOREIGN KEY (`class_group_id`) REFERENCES `class_groups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `class_group_students`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_group_students` (
  `class_group_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  KEY `FKaticlcef2mf3idih3tgyxu617` (`student_id`),
  KEY `FK1q4q9y24xbs45xvfrubh0uumj` (`class_group_id`),
  CONSTRAINT `FK1q4q9y24xbs45xvfrubh0uumj` FOREIGN KEY (`class_group_id`) REFERENCES `class_groups` (`id`),
  CONSTRAINT `FKaticlcef2mf3idih3tgyxu617` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `class_groups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_34jg0ay5tduvfcvvjvjc2dio9` (`name`),
  KEY `FKlekewp8nnpbvpobf73i8g0cgp` (`teacher_id`),
  CONSTRAINT `FKlekewp8nnpbvpobf73i8g0cgp` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_assignees`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_assignees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) DEFAULT NULL,
  `goal_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK392tk4m8w5ct9h8ibh3daw38s` (`goal_id`,`student_id`),
  KEY `FK2bnlp9fc5wogy9wxvdwdujvuf` (`student_id`),
  CONSTRAINT `FK2bnlp9fc5wogy9wxvdwdujvuf` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK76cywx4i59b46xaa09biys732` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_assignments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `assignment_id` bigint NOT NULL,
  `goal_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9srl64gh0f6ts4ef72u06ctg3` (`goal_id`,`assignment_id`),
  KEY `FKpfshupf9am21mcbh99vdl2bll` (`assignment_id`),
  CONSTRAINT `FK1eafxhrs593ditj3mpkmjoigs` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`),
  CONSTRAINT `FKpfshupf9am21mcbh99vdl2bll` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_comments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `image_urls` text,
  `updated_at` datetime(6) DEFAULT NULL,
  `goal_id` bigint NOT NULL,
  `student_id` bigint DEFAULT NULL,
  `author_role` varchar(10) NOT NULL,
  `target_student_id` bigint DEFAULT NULL,
  `visibility` varchar(20) NOT NULL,
  `author_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbp36bu9h36a7moitslkxd3hd9` (`goal_id`),
  KEY `idx_gc_target` (`target_student_id`),
  KEY `fk_gc_author` (`author_id`),
  KEY `FK38w7p15ilt74jg8w28dff08nt` (`student_id`),
  CONSTRAINT `FK38w7p15ilt74jg8w28dff08nt` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_gc_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKbp36bu9h36a7moitslkxd3hd9` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_instance_assignments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_instance_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `assignment_id` bigint NOT NULL,
  `goal_instance_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbvpm1rnxx35nwwcu7g4736m1c` (`assignment_id`),
  KEY `FKq2nnyvpkemru5xy052rcyuf7k` (`goal_instance_id`),
  CONSTRAINT `FKbvpm1rnxx35nwwcu7g4736m1c` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `FKq2nnyvpkemru5xy052rcyuf7k` FOREIGN KEY (`goal_instance_id`) REFERENCES `goal_instances` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_instance_items`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_instance_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `target_date` date DEFAULT NULL,
  `weight` int NOT NULL DEFAULT '1',
  `actual_start_date` date DEFAULT NULL,
  `actual_end_date` date DEFAULT NULL,
  `progress` int NOT NULL DEFAULT '0',
  `status` enum('NOT_STARTED','IN_PROGRESS','COMPLETED','OVERDUE') NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_goal_instance_items_instance` (`instance_id`),
  CONSTRAINT `fk_goal_instance_items_instance` FOREIGN KEY (`instance_id`) REFERENCES `goal_instances` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_instances`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_instances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `manager_id` bigint NOT NULL,
  `assignee_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `planned_start_date` date DEFAULT NULL,
  `planned_end_date` date DEFAULT NULL,
  `actual_start_date` date DEFAULT NULL,
  `actual_end_date` date DEFAULT NULL,
  `progress` int NOT NULL DEFAULT '0',
  `status` enum('NOT_STARTED','IN_PROGRESS','COMPLETED','OVERDUE') NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_goal_instances_template` (`template_id`),
  KEY `fk_goal_instances_manager` (`manager_id`),
  KEY `fk_goal_instances_assignee` (`assignee_id`),
  CONSTRAINT `fk_goal_instances_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_goal_instances_manager` FOREIGN KEY (`manager_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_goal_instances_template` FOREIGN KEY (`template_id`) REFERENCES `goal_templates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_template_items`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_template_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `target_date` date DEFAULT NULL,
  `weight` int NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_goal_template_items_template` (`template_id`),
  CONSTRAINT `fk_goal_template_items_template` FOREIGN KEY (`template_id`) REFERENCES `goal_templates` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `creator_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_goal_templates_creator` (`creator_id`),
  CONSTRAINT `fk_goal_templates_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goals`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actual_end` date DEFAULT NULL,
  `actual_start` date DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `owners` varchar(500) DEFAULT NULL,
  `planned_end` date DEFAULT NULL,
  `planned_start` date DEFAULT NULL,
  `progress` int NOT NULL,
  `status` enum('TODO','IN_PROGRESS','DONE','LATE') NOT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `depth` int NOT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `class_group_id` bigint DEFAULT NULL,
  `copyable` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKg3itcwfkjytngckb9its3m6lb` (`parent_id`),
  KEY `FKk32u9c4245y1kvp2pakg6o9mq` (`assignee_id`),
  KEY `FKr5sk1nsiceoec7f0a1846thag` (`manager_id`),
  KEY `FK1ys5lvhh4oo4qn82xj6i14mol` (`class_group_id`),
  CONSTRAINT `FK1ys5lvhh4oo4qn82xj6i14mol` FOREIGN KEY (`class_group_id`) REFERENCES `class_groups` (`id`),
  CONSTRAINT `FKg3itcwfkjytngckb9its3m6lb` FOREIGN KEY (`parent_id`) REFERENCES `goals` (`id`),
  CONSTRAINT `FKk32u9c4245y1kvp2pakg6o9mq` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr5sk1nsiceoec7f0a1846thag` FOREIGN KEY (`manager_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_tags`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_tags` (
  `sort_order` int DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `chapter` varchar(64) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `math_goal_subgoals`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `math_goal_subgoals` (
  `math_goal_id` bigint NOT NULL,
  `subgoal` varchar(500) DEFAULT NULL,
  KEY `FK6ueo315vvymoywxryx7jd6rnh` (`math_goal_id`),
  CONSTRAINT `FK6ueo315vvymoywxryx7jd6rnh` FOREIGN KEY (`math_goal_id`) REFERENCES `math_goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `math_goals`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `math_goals` (
  `progress` int NOT NULL,
  `target_date` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `category` varchar(50) NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mistake_notes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mistake_notes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `source_assignment_id` bigint DEFAULT NULL,
  `note_content` text COLLATE utf8mb4_unicode_ci,
  `image_urls_json` mediumtext COLLATE utf8mb4_unicode_ci,
  `mastery` enum('UNREVIEWED','REVIEWING','MASTERED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mn_student_question` (`student_id`,`question_id`),
  KEY `idx_mn_student` (`student_id`),
  KEY `idx_mn_mastery` (`student_id`,`mastery`),
  KEY `fk_mn_question` (`question_id`),
  CONSTRAINT `fk_mn_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mn_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_knowledge_tags`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_knowledge_tags` (
  `question_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`question_id`,`tag_id`),
  KEY `FKt6n627b4xte9wi1x1d6xhe76t` (`tag_id`),
  CONSTRAINT `FKekvgk470tai0ccjen8f4bwblu` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `FKt6n627b4xte9wi1x1d6xhe76t` FOREIGN KEY (`tag_id`) REFERENCES `knowledge_tags` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_options`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_options` (
  `is_correct` bit(1) NOT NULL,
  `option_label` varchar(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `content_latex` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsb9v00wdrgc9qojtjkv7e1gkp` (`question_id`),
  CONSTRAINT `FKsb9v00wdrgc9qojtjkv7e1gkp` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_shares`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_shares` (
  `question_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`question_id`,`user_id`),
  KEY `idx_qs_user` (`user_id`),
  CONSTRAINT `fk_qs_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `questions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `questions` (
  `difficulty` int NOT NULL,
  `total_score` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `visibility` enum('PUBLIC','SHARED','PRIVATE') NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `source` varchar(128) DEFAULT NULL,
  `answer_key` text,
  `content_latex` text NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `question_type` enum('SINGLE_CHOICE','FILL_BLANK','OPEN_ENDED') NOT NULL,
  `image_urls_json` mediumtext COMMENT '题目配图（JSON 数组，Base64 DataURL）',
  PRIMARY KEY (`id`),
  KEY `idx_q_visibility` (`visibility`),
  KEY `idx_q_created_by` (`created_by`),
  CONSTRAINT `FKmiy2i24hb8838iqkm2kunw286` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solution_steps`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solution_steps` (
  `step_order` int NOT NULL,
  `step_score` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `common_errors` text,
  `content_latex` text NOT NULL,
  `image_urls_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`),
  KEY `FK1nhacfwnfmwmunx9cfflq2kx` (`question_id`),
  CONSTRAINT `FK1nhacfwnfmwmunx9cfflq2kx` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_answers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_answers` (
  `auto_score` int DEFAULT NULL,
  `score` int DEFAULT NULL,
  `assignment_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `answer_content` text,
  `feedback` text,
  `image_url` varchar(255) DEFAULT NULL,
  `error_type` enum('CONCEPT','CALC','READING','NONE') DEFAULT NULL,
  `status` enum('DRAFT','SUBMITTED','AUTO_GRADED','REVIEWED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `image_urls_json` mediumtext,
  PRIMARY KEY (`id`),
  KEY `FKbv76tn974au3d1dj9mg9m06qd` (`assignment_id`),
  KEY `FK8nyksamccim8emu803uhf2da` (`question_id`),
  KEY `FKg1mxchl6myi06wb187ebjoutn` (`reviewer_id`),
  KEY `idx_sa_student_question` (`student_id`,`question_id`),
  CONSTRAINT `FK8nyksamccim8emu803uhf2da` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `FKbv76tn974au3d1dj9mg9m06qd` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `FKg1mxchl6myi06wb187ebjoutn` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkjgcncx8ssaj23jtydpwwc0up` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_goal_progress`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_goal_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `progress` int NOT NULL,
  `status` enum('TODO','IN_PROGRESS','DONE','LATE') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `goal_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `actual_end` date DEFAULT NULL,
  `actual_start` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa7prp3hq9hnkfnccbj7vijr8o` (`goal_id`,`student_id`),
  KEY `FKi1kdvsugs6gryh720erswk2bi` (`student_id`),
  CONSTRAINT `FKi1kdvsugs6gryh720erswk2bi` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKk3nslmvtfe4palbnfnx6udeta` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teacher_class_groups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_class_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_group_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbpifo00fqlllvhkcsfgfs1xjw` (`teacher_id`,`class_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `class_name` varchar(64) DEFAULT NULL,
  `real_name` varchar(64) DEFAULT NULL,
  `username` varchar(100) NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','TEACHER','STUDENT') NOT NULL,
  `class_group_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-18 15:06:53

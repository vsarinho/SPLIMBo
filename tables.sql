-- MySQL Script generated by MySQL Workbench
-- Sáb 30 Jul 2016 17:40:18 BRT
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema zapserver
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `zapserver` ;

-- -----------------------------------------------------
-- Schema zapserver
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `zapserver` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `zapserver` ;

-- -----------------------------------------------------
-- Table `zapserver`.`Queue`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `zapserver`.`Queue` ;

CREATE TABLE IF NOT EXISTS `zapserver`.`Queue` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `jidServer` VARCHAR(200) NULL,
  `jidClient` VARCHAR(200) NULL,
  `url` VARCHAR(1000) NULL,
  `message` TEXT NULL,
  `data` LONGBLOB NULL,
  `imageLabel` VARCHAR(200) NULL,
  `extension` VARCHAR(10) NULL,
  `status` VARCHAR(1) NULL,
  `dateTime` DATETIME NULL,
  `dateTimeToSend` DATETIME NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `zapserver`.`Log`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `zapserver`.`Log` ;

CREATE TABLE IF NOT EXISTS `zapserver`.`Log` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `jidServer` VARCHAR(200) NULL,
  `jidClient` VARCHAR(200) NULL,
  `url` VARCHAR(1000) NULL,
  `message` TEXT NULL,
  `extension` VARCHAR(10) NULL,
  `status` VARCHAR(1) NULL,
  `dateTime` DATETIME NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;


alter database zapserver character set = utf8mb4 collate = utf8mb4_unicode_ci;

alter table zapserver.Queue convert to charset utf8mb4 collate utf8mb4_unicode_ci;
alter table zapserver.Log convert to charset utf8mb4 collate utf8mb4_unicode_ci;

alter table zapserver.Queue change message message TEXT character set utf8mb4  collate utf8mb4_unicode_ci;
alter table zapserver.Log change message message TEXT character set utf8mb4  collate utf8mb4_unicode_ci;

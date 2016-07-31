
drop database if exists libraszap;
create database libraszap;

use libraszap;

DROP TABLE IF EXISTS Jogada;
CREATE TABLE Jogada (
	id int NOT NULL AUTO_INCREMENT,
	jidCliente VARCHAR(80) CHARACTER SET 'utf8',
	idCurrentVideo int,
	opcao1 VARCHAR(80) CHARACTER SET 'utf8',
	opcao2 VARCHAR(80) CHARACTER SET 'utf8',
	opcao3 VARCHAR(80) CHARACTER SET 'utf8',
	opcao4 VARCHAR(80) CHARACTER SET 'utf8',
	opcaoCorreta VARCHAR(80) CHARACTER SET 'utf8',
	PRIMARY KEY (id)
)ENGINE=InnoDb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS Videos;
CREATE TABLE Videos (
	id int NOT NULL AUTO_INCREMENT,
	name VARCHAR(80) CHARACTER SET 'utf8',
	url VARCHAR(500) CHARACTER SET 'utf8',
	PRIMARY KEY (id)
)ENGINE=InnoDb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS Resultados;
CREATE TABLE Resultados (
	id int NOT NULL AUTO_INCREMENT,
	jidCliente VARCHAR(80) CHARACTER SET 'utf8',
	nome VARCHAR(10) CHARACTER SET 'utf8',
	score int,
  PRIMARY KEY (id)
)ENGINE=InnoDb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS Partidas;
CREATE TABLE Partidas (
	id int NOT NULL AUTO_INCREMENT,
	jidCliente VARCHAR(80) CHARACTER SET 'utf8',
	score int,
	dataHora datetime,
  PRIMARY KEY (id)
)ENGINE=InnoDb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS Pesquisa;
CREATE TABLE Pesquisa (
	id int NOT NULL AUTO_INCREMENT,
	jidCliente VARCHAR(80) CHARACTER SET 'utf8',
	conhecimento VARCHAR(5000) CHARACTER SET 'utf8',
	utilidade VARCHAR(5000) CHARACTER SET 'utf8',
	dificuldade VARCHAR(5000) CHARACTER SET 'utf8',
	satisfacao VARCHAR(5000) CHARACTER SET 'utf8',
	dataHora datetime,
  PRIMARY KEY (id)
)ENGINE=InnoDb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS TempOpcoes;
CREATE TABLE TempOpcoes (
	id int NOT NULL AUTO_INCREMENT,
	idVideo int,
	idUsuario VARCHAR(80) CHARACTER SET 'utf8',
  PRIMARY KEY (id)
)ENGINE=MEMORY DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS TempHallDaFama;
CREATE TABLE TempHallDaFama (
	id int NOT NULL AUTO_INCREMENT,
	posicao int,
	nome  VARCHAR(10) CHARACTER SET 'utf8',
	score int,
	idUsuario VARCHAR(80) CHARACTER SET 'utf8',
  PRIMARY KEY (id)
)ENGINE=MEMORY DEFAULT CHARSET=utf8;

delimiter //

drop PROCEDURE if exists prepararJogada //
CREATE PROCEDURE prepararJogada(IN p_jidCliente VARCHAR(80) CHARACTER SET 'utf8')
BEGIN
	DECLARE v_idVideo1 INT DEFAULT 0;
	DECLARE v_idVideo2 INT DEFAULT 0;
	DECLARE v_idVideo3 INT DEFAULT 0;
	DECLARE v_idVideo4 INT DEFAULT 0;
	DECLARE v_idVideoCorreto int;
	
	-- limpa jogadas anteriores
	delete from Jogada where jidCliente  = p_jidCliente;
	
	-- escolhes 2 videos randomicamente  da lista de videos
	SELECT id into v_idVideo1 FROM Videos ORDER BY RAND() LIMIT 1;
	SELECT id into v_idVideo2 FROM Videos where name not in (select name from Videos V where V.id = v_idVideo1) ORDER BY RAND() LIMIT 1;
	SELECT id into v_idVideo3 FROM Videos where name not in (select name from Videos V where V.id in (v_idVideo1,v_idVideo2)) ORDER BY RAND() LIMIT 1;
	SELECT id into v_idVideo4 FROM Videos where name not in (select name from Videos V where V.id in (v_idVideo1,v_idVideo2,v_idVideo3)) ORDER BY RAND() LIMIT 1;
	set v_idVideoCorreto := v_idVideo1;
		
	-- embaralha as opcoes de video obtidas
	delete from TempOpcoes where idUsuario  = p_jidCliente;
	insert into TempOpcoes (idVideo, idUsuario) values (v_idVideo1, p_jidCliente);
	insert into TempOpcoes (idVideo, idUsuario) values (v_idVideo2, p_jidCliente);	
	insert into TempOpcoes (idVideo, idUsuario) values (v_idVideo3, p_jidCliente);	
	insert into TempOpcoes (idVideo, idUsuario) values (v_idVideo4, p_jidCliente);	
	SELECT idVideo into v_idVideo1 FROM TempOpcoes where idUsuario = p_jidCliente ORDER BY RAND() LIMIT 1;
	SELECT idVideo into v_idVideo2 FROM TempOpcoes where idUsuario = p_jidCliente and idVideo <> v_idVideo1 ORDER BY RAND() LIMIT 1;
	SELECT idVideo into v_idVideo3 FROM TempOpcoes where idUsuario = p_jidCliente and idVideo not in (v_idVideo1,v_idVideo2) ORDER BY RAND() LIMIT 1;
	SELECT idVideo into v_idVideo4 FROM TempOpcoes where idUsuario = p_jidCliente and idVideo not in (v_idVideo1,v_idVideo2,v_idVideo3) ORDER BY RAND() LIMIT 1;				
	
	-- insere nova jogada para o jidCliente	
	insert into Jogada (jidCliente, idCurrentVideo, opcao1, opcao2, opcao3, opcao4, opcaoCorreta)
	values (p_jidCliente, v_idVideoCorreto, (select name from Videos where id = v_idVideo1),
						(select name from Videos where id = v_idVideo2),
						(select name from Videos where id = v_idVideo3),
						(select name from Videos where id = v_idVideo4),	
						(select name from Videos where id = v_idVideoCorreto));
END //  

drop PROCEDURE if exists confirmarAcerto //
CREATE PROCEDURE confirmarAcerto(IN p_opcao VARCHAR(30) CHARACTER SET 'utf8',
								IN p_jidCliente VARCHAR(80) CHARACTER SET 'utf8')
BEGIN
	select if(opcaoCorreta = p_opcao, 'true', 'false') from Jogada where jidCliente = p_jidCliente;
END //  

drop PROCEDURE if exists posicaoHalldaFama //
CREATE PROCEDURE posicaoHalldaFama(IN p_score VARCHAR(4) CHARACTER SET 'utf8')
BEGIN
	if (p_score > 0) then
		select count(*)+1 from Resultados where score >= p_score;	
	else
		select 20;
	end if;
END //

drop PROCEDURE if exists registrarResultado //
CREATE PROCEDURE registrarResultado(IN p_jidCliente VARCHAR(80) CHARACTER SET 'utf8',
									IN p_nome VARCHAR(5000) CHARACTER SET 'utf8',
									IN p_score VARCHAR(6) CHARACTER SET 'utf8')
BEGIN
	insert into Resultados (jidCliente, nome, score)
	values (p_jidCliente, SUBSTRING(p_nome,1,10), p_score);	
END // 

drop PROCEDURE if exists registrarPartida //
CREATE PROCEDURE registrarPartida(IN p_jidCliente VARCHAR(80) CHARACTER SET 'utf8',
									IN p_score VARCHAR(6) CHARACTER SET 'utf8')
BEGIN
	insert into Partidas (jidCliente, score, dataHora)
	values (p_jidCliente, p_score, now());	
END // 

drop PROCEDURE if exists registrarPesquisa //
CREATE PROCEDURE registrarPesquisa(IN p_jidCliente VARCHAR(80) CHARACTER SET 'utf8',
					IN p_conhecimento VARCHAR(5000) CHARACTER SET 'utf8',
					IN p_utilidade VARCHAR(5000) CHARACTER SET 'utf8',
					IN p_dificuldade VARCHAR(5000) CHARACTER SET 'utf8',
					IN p_satisfacao VARCHAR(5000) CHARACTER SET 'utf8')
BEGIN
	insert into Pesquisa (jidCliente, conhecimento, utilidade, dificuldade, satisfacao, dataHora)
	values (p_jidCliente, p_conhecimento, p_utilidade, p_dificuldade, p_satisfacao, now());	
	
END // 

drop PROCEDURE if exists obterHallDaFama //
CREATE PROCEDURE obterHallDaFama(IN p_jidCliente VARCHAR(80) CHARACTER SET 'utf8')
BEGIN
	DECLARE v_result VARCHAR(5000) CHARACTER SET 'utf8';
	
	delete from TempHallDaFama where idUsuario = p_jidCliente;
	SET @cnt := 0;
	insert into TempHallDaFama (posicao, nome, score, idUsuario)
	select @cnt := @cnt + 1, nome, score, p_jidCliente
	FROM Resultados order by score desc limit 10;
	
	SELECT group_concat(concat(C.posicao, ': ', C.nome, ' - ', C.score) separator '\n')
	into v_result
	FROM (select * from TempHallDaFama where idUsuario = p_jidCliente order by id) C order by C.posicao;

	if (v_result is not null) then
		SELECT concat('### Melhores Resultados ###\n', v_result);
	else
		SELECT '### Melhores Resultados ###\n';
	end if;
END //

delimiter ;
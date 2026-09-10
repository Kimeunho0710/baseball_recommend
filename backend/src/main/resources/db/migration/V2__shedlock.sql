-- ============================================================
-- V2: ShedLock 잠금 테이블
--  다중 인스턴스(ECS task / EC2 오토스케일링)에서 @Scheduled 가 동시에 돌아
--  KBO 스크래핑이 중복 실행되는 것을 막기 위한 분산 락 저장소
-- ============================================================

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB;

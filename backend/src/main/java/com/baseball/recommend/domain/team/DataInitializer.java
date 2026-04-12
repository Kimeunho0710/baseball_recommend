package com.baseball.recommend.domain.team;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final TeamRepository teamRepository;

    @Override
    public void run(ApplicationArguments args) {

        List<Team> teams = List.of(
            Team.builder()
                .name("KIA 타이거즈")
                .city("광주")
                .stadium("광주-기아 챔피언스 필드")
                .description("KBO 최다 우승 팀. 해태 타이거즈 시절부터 이어온 뜨거운 팬덤과 전통의 명문 구단. 2024년 한국시리즈 우승으로 역대 최다 11회 우승을 달성했다.")
                .characteristics("전통, 우승 DNA, 열정적 팬덤, 역동적, 강인함")
                .primaryColor("#EA0029")
                .foundedYear(1982)
                .championships(11)
                .mascot("호랑이")
                .manager("이범호")
                .build(),
            Team.builder()
                .name("삼성 라이온즈")
                .city("대구")
                .stadium("대구 삼성 라이온즈 파크")
                .description("체계적이고 안정적인 운영으로 명성을 쌓은 기업형 명문 구단. 2010년대 4연패를 달성하며 왕조를 구축했고, 탄탄한 팜 시스템으로 유명하다.")
                .characteristics("체계적, 안정적, 명문, 균형, 꼼꼼함")
                .primaryColor("#074CA1")
                .foundedYear(1982)
                .championships(8)
                .mascot("사자")
                .manager("박진만")
                .build(),
            Team.builder()
                .name("LG 트윈스")
                .city("서울")
                .stadium("잠실 야구장")
                .description("서울의 화려함과 트렌디한 감각을 지닌 젊고 세련된 구단. 2023년 29년 만의 한국시리즈 우승을 달성하며 팬들의 한을 풀었다.")
                .characteristics("화려함, 트렌디, 젊음, 세련됨, 서울 감성")
                .primaryColor("#C30452")
                .foundedYear(1990)
                .championships(2)
                .mascot("쌍둥이")
                .manager("염경엽")
                .build(),
            Team.builder()
                .name("두산 베어스")
                .city("서울")
                .stadium("잠실 야구장")
                .description("강인한 저력과 파워로 위기에 강한 뚝심있는 명문 구단. OB 베어스 시절부터 이어온 강팀 이미지와 2010년대 꾸준한 한국시리즈 진출로 명성을 쌓았다.")
                .characteristics("강인함, 파워, 뚝심, 저력, 위기 극복")
                .primaryColor("#131230")
                .foundedYear(1982)
                .championships(6)
                .mascot("곰")
                .manager("이승엽")
                .build(),
            Team.builder()
                .name("KT 위즈")
                .city("수원")
                .stadium("수원 KT 위즈 파크")
                .description("2015년 창단한 막내 구단. 도전 정신과 혁신으로 빠르게 성장해 2021년 창단 7년 만에 첫 한국시리즈 우승을 차지했다.")
                .characteristics("도전, 혁신, 젊음, 성장, 활기")
                .primaryColor("#000000")
                .foundedYear(2015)
                .championships(1)
                .mascot("마법사")
                .manager("이강철")
                .build(),
            Team.builder()
                .name("SSG 랜더스")
                .city("인천")
                .stadium("SSG 랜더스 필드")
                .description("SK 와이번스 시절부터 이어온 강팀 DNA를 바탕으로 화끈한 공격 야구와 스타 플레이어들로 재미와 활력이 넘치는 구단.")
                .characteristics("공격적, 화끈함, 스타 플레이어, 재미, 활력")
                .primaryColor("#CE0E2D")
                .foundedYear(2000)
                .championships(5)
                .mascot("랜더")
                .manager("이숭용")
                .build(),
            Team.builder()
                .name("롯데 자이언츠")
                .city("부산")
                .stadium("사직 야구장")
                .description("부산 감성과 한이 담긴 스토리, 열정 가득한 팬덤을 자랑하는 구단. 갈매기 응원단과 함께하는 사직구장의 뜨거운 분위기는 KBO 최고로 꼽힌다.")
                .characteristics("열정, 부산 감성, 한(恨), 감동 스토리, 끈끈한 팬덤")
                .primaryColor("#041E42")
                .foundedYear(1975)
                .championships(2)
                .mascot("갈매기")
                .manager("김태형")
                .build(),
            Team.builder()
                .name("한화 이글스")
                .city("대전")
                .stadium("한화생명 이글스 파크")
                .description("우직하고 끈기 있는 팬과의 유대감이 깊은 감동적인 응원 문화의 구단. 대전을 연고로 한 독보적인 팬 문화와 함께 재건을 향해 나아가고 있다.")
                .characteristics("우직함, 끈기, 팬과의 유대감, 감동, 응원 문화")
                .primaryColor("#FF6600")
                .foundedYear(1986)
                .championships(1)
                .mascot("독수리")
                .manager("김경문")
                .build(),
            Team.builder()
                .name("NC 다이노스")
                .city("창원")
                .stadium("창원 NC 파크")
                .description("2012년 창단해 데이터와 분석 기반의 스마트한 야구로 빠르게 성장한 구단. 2020년 창단 9년 만에 한국시리즈 우승을 차지했다.")
                .characteristics("스마트, 데이터 분석, 체계적, 전략적, 선수 육성")
                .primaryColor("#071D49")
                .foundedYear(2012)
                .championships(1)
                .mascot("공룡")
                .manager("강인권")
                .build(),
            Team.builder()
                .name("키움 히어로즈")
                .city("서울")
                .stadium("고척 스카이돔")
                .description("2008년 창단해 저비용 고효율의 도전적인 구단 운영으로 유명하다. 국내 유일의 돔구장인 고척 스카이돔을 홈으로 사용하며 젊고 개성 넘치는 야구를 펼친다.")
                .characteristics("젊음, 화끈한 타격, 저비용 고효율, 개성, 도전")
                .primaryColor("#820024")
                .foundedYear(2008)
                .championships(0)
                .mascot("영웅")
                .manager("홍원기")
                .build()
        );

        for (Team teamData : teams) {
            teamRepository.findByName(teamData.getName()).ifPresentOrElse(
                existing -> {
                    existing.update(
                        teamData.getCity(), teamData.getStadium(), teamData.getDescription(),
                        teamData.getCharacteristics(), teamData.getPrimaryColor(),
                        teamData.getFoundedYear(), teamData.getChampionships(),
                        teamData.getMascot(), teamData.getManager()
                    );
                    teamRepository.save(existing);
                },
                () -> teamRepository.save(teamData)
            );
        }
        log.info("KBO 10개 팀 데이터 초기화/업데이트 완료");
    }
}

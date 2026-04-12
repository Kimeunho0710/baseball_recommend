package com.baseball.recommend.domain.team;

import com.baseball.recommend.domain.player.Player;
import com.baseball.recommend.domain.player.PlayerRepository;
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
    private final PlayerRepository playerRepository;

    @Override
    public void run(ApplicationArguments args) {
        initTeams();
        initPlayers();
    }

    // ─────────────────────────────────────────────
    // 팀 초기화
    // ─────────────────────────────────────────────

    private void initTeams() {
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
                .beginnerGuide("KBO 역사를 함께 느끼고 싶다면 KIA를 추천해요. 광주 홈경기 응원 문화는 KBO 최고 수준이에요. 2024 MVP 김도영의 폭발적인 플레이와 최다 우승 팀의 우승 DNA를 현장에서 느껴보세요.")
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
                .beginnerGuide("야구의 전술과 전략을 분석하며 즐기고 싶다면 삼성을 추천해요. 탄탄한 팜 시스템과 체계적 구단 운영 방식이 매력이에요. 원태인의 정교한 투구와 구자욱의 안타 행진을 주목해보세요.")
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
                .beginnerGuide("서울 잠실의 화려한 분위기와 세련된 응원 문화를 원한다면 LG를 추천해요. 2023년 29년 만의 우승을 이룬 팀으로, 오지환·박해민 등 팀의 아이콘들을 직접 볼 수 있어요.")
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
                .beginnerGuide("강인한 투지와 역전 야구를 좋아한다면 두산을 추천해요. 포기를 모르는 뚝심 야구가 매력이에요. 레전드 이승엽 감독이 이끄는 두산의 부활을 함께 응원해보세요.")
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
                .beginnerGuide("빠르게 성장하는 팀을 응원하고 싶다면 KT를 추천해요. KBO 막내 구단이 7년 만에 우승을 차지한 스토리는 정말 드라마 같아요. 강백호의 폭발적인 타격과 고영표의 에이스 투구를 주목해보세요.")
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
                .beginnerGuide("스타 플레이어들의 화끈한 야구를 원한다면 SSG를 추천해요. 홈런왕 최정과 레전드 투수 김광현이 함께하는 팀이에요. 인천 SSG 랜더스 필드의 응원 분위기도 정말 뜨겁답니다.")
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
                .beginnerGuide("야구장 분위기를 제대로 경험하고 싶다면 롯데 사직구장을 추천해요. KBO 최고의 응원 문화로 유명해요. 갈매기 응원단과 함께 목터지게 응원하다 보면 진정한 야구팬이 될 거예요.")
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
                .beginnerGuide("레전드 류현진의 투구를 직접 볼 수 있는 지금이 한화 팬이 되기 딱 좋은 타이밍이에요. 대전 팬들의 열정적이고 끈끈한 응원 문화는 독보적이에요. 팀의 부활을 함께 만들어가는 재미가 있어요.")
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
                .beginnerGuide("스마트하고 전략적인 야구를 즐기고 싶다면 NC를 추천해요. 데이터 기반의 팀 운영이 인상적이에요. 손아섭·박민우 등 베테랑들의 경험과 젊은 선수들의 패기가 조화로운 팀이에요.")
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
                .beginnerGuide("국내 유일의 돔구장 고척에서 날씨 걱정 없이 야구를 즐겨보세요. 저비용 고효율의 도전적 구단 운영 방식이 재미있어요. KBO 차세대 스타들이 모여있는 젊고 역동적인 팀이에요.")
                .build()
        );

        for (Team teamData : teams) {
            teamRepository.findByName(teamData.getName()).ifPresentOrElse(
                existing -> {
                    existing.update(
                        teamData.getCity(), teamData.getStadium(), teamData.getDescription(),
                        teamData.getCharacteristics(), teamData.getPrimaryColor(),
                        teamData.getFoundedYear(), teamData.getChampionships(),
                        teamData.getMascot(), teamData.getManager(), teamData.getBeginnerGuide()
                    );
                    teamRepository.save(existing);
                },
                () -> teamRepository.save(teamData)
            );
        }
        log.info("KBO 10개 팀 데이터 초기화/업데이트 완료");
    }

    // ─────────────────────────────────────────────
    // 선수 초기화 (팀당 4명, 총 40명)
    // ─────────────────────────────────────────────

    private void initPlayers() {
        teamRepository.findAll().forEach(team -> {
            if (playerRepository.existsByTeamId(team.getId())) return;

            List<Player> players = switch (team.getName()) {
                case "KIA 타이거즈" -> List.of(
                    player(team, "양현종", "투수", 54, "KIA의 영원한 에이스. 25년 이상 한 팀에서 활약한 프랜차이즈 스타로, 뛰어난 제구력과 노련한 피칭이 강점이다."),
                    player(team, "김도영", "내야수", 5, "2024 KBO MVP 및 30-30(홈런·도루) 달성. 폭발적인 잠재력과 압도적인 활약으로 KBO 차세대 최고 스타로 손꼽힌다."),
                    player(team, "나성범", "외야수", 7, "2024 한국시리즈 우승의 핵심 멤버. 경험과 파워를 갖춘 중심 타자로 클러치 상황에서 강한 면모를 보인다."),
                    player(team, "최형우", "지명타자", 35, "KBO 레전드 타자. 통산 2000안타를 넘어선 대타자로, 풍부한 경험과 리더십으로 팀을 이끈다.")
                );
                case "삼성 라이온즈" -> List.of(
                    player(team, "원태인", "투수", 35, "삼성 재건의 핵심 에이스. 안정적인 제구와 다양한 구종으로 타자를 압도하는 완성형 선발 투수다."),
                    player(team, "구자욱", "외야수", 40, "삼성의 얼굴이자 중심 타자. 뛰어난 타격 기술과 넓은 수비 범위를 겸비한 팀의 핵심이다."),
                    player(team, "강민호", "포수", 22, "KBO를 대표하는 포수. 베테랑 리더십과 강력한 송구로 투수진을 이끄는 팀의 정신적 지주다."),
                    player(team, "이재현", "내야수", 23, "삼성의 차세대 유망주. 뛰어난 수비와 빠른 발, 성장하는 타격이 기대되는 젊은 내야수다.")
                );
                case "LG 트윈스" -> List.of(
                    player(team, "오지환", "내야수", 5, "LG의 영원한 아이콘이자 주장. 카리스마 넘치는 리더십과 화끈한 스윙으로 팬들의 절대적 사랑을 받는다."),
                    player(team, "박해민", "외야수", 10, "LG의 리드오프. 빠른 발과 넓은 수비 범위, 날카로운 선구안으로 팀의 공격 흐름을 만드는 핵심 선수다."),
                    player(team, "문보경", "내야수", 33, "2023 우승의 주역이자 LG 타선의 중심. 강한 파워와 클러치 능력으로 팬들을 열광시키는 타자다."),
                    player(team, "임찬규", "투수", 37, "LG 투수진의 중심. 다양한 구종과 안정적인 제구로 이닝을 소화하는 믿음직한 선발 투수다.")
                );
                case "두산 베어스" -> List.of(
                    player(team, "양석환", "내야수", 6, "두산의 주장이자 중심 타자. 뛰어난 클러치 능력과 강한 파워로 두산 타선을 이끄는 핵심 베테랑이다."),
                    player(team, "정수빈", "외야수", 29, "두산의 리드오프. 빠른 발과 날카로운 선구안으로 끊임없이 득점 기회를 만드는 공격의 시작점이다."),
                    player(team, "김재환", "외야수", 22, "두산 타선의 핵심 슬러거. 강한 파워와 승부 근성을 자랑하며 결정적 순간에 장타를 만들어낸다."),
                    player(team, "곽빈", "투수", 11, "두산의 젊은 에이스. 힘 있는 직구와 날카로운 변화구로 두산 투수진의 미래를 이끄는 선발 투수다.")
                );
                case "KT 위즈" -> List.of(
                    player(team, "강백호", "내야수", 45, "KT 최고의 스타. 폭발적인 장타력과 화끈한 스윙이 매력인 KT 타선의 핵심이자 팬들이 가장 사랑하는 선수다."),
                    player(team, "고영표", "투수", 29, "KT의 에이스. 안정적인 제구력과 다양한 구종으로 타자를 요리하는 KT 투수진의 중심이다."),
                    player(team, "박영현", "투수", 30, "KT의 마무리. 강력한 직구와 침착한 마무리 능력으로 팬들이 믿고 보는 수호신이다."),
                    player(team, "황재균", "내야수", 10, "KT의 베테랑 리더. 안정적인 3루 수비와 풍부한 경험으로 팀의 정신적 지주 역할을 한다.")
                );
                case "SSG 랜더스" -> List.of(
                    player(team, "최정", "내야수", 13, "KBO 역대 홈런 신기록 보유자. SSG의 레전드 슬러거로, 압도적인 파워와 불굴의 의지가 매력이다."),
                    player(team, "김광현", "투수", 34, "SSG의 에이스이자 KBO 레전드. MLB 경험을 바탕으로 한 완성형 투구와 믿음직한 경기 운영이 강점이다."),
                    player(team, "박성한", "내야수", 51, "SSG의 주전 유격수. 빠른 발과 넓은 수비 범위, 성장하는 타격으로 SSG의 핵심 선수로 자리잡았다."),
                    player(team, "한유섬", "외야수", 36, "SSG의 중심 타자. 강한 파워 히팅과 안정적인 외야 수비로 팀의 공격과 수비를 책임진다.")
                );
                case "롯데 자이언츠" -> List.of(
                    player(team, "전준우", "외야수", 31, "롯데의 베테랑 중심 타자. 클러치 능력과 리더십이 뛰어나며 부산 팬들의 절대적인 신뢰를 받는 선수다."),
                    player(team, "노진혁", "내야수", 36, "롯데의 주장이자 베테랑 내야수. 안정적인 수비와 선구안 좋은 타격으로 팀의 균형을 잡아준다."),
                    player(team, "빅터 레예스", "외야수", 7, "롯데의 외국인 타자. 강한 파워와 넓은 수비 범위로 롯데 타선에 활력을 불어넣는다."),
                    player(team, "윤동희", "외야수", 27, "롯데의 차세대 스타. 빠른 발과 폭발적인 스윙으로 성장 중인 젊은 외야수다.")
                );
                case "한화 이글스" -> List.of(
                    player(team, "류현진", "투수", 99, "KBO 복귀 후 한화의 간판 에이스. MLB에서 검증된 세계 수준의 투구 기술과 경험으로 한화 투수진을 이끈다."),
                    player(team, "노시환", "내야수", 5, "한화의 홈런 타자. 폭발적인 장타력으로 팬들을 열광시키는 한화 타선의 핵심이자 한화 재건의 주인공이다."),
                    player(team, "채은성", "외야수", 30, "한화의 주력 외야수. 안정적인 타격과 수비로 한화 타선을 지탱하는 믿음직한 베테랑이다."),
                    player(team, "문동주", "투수", 26, "한화의 차세대 에이스. 강속구와 뛰어난 잠재력을 지닌 젊은 투수로 한화의 밝은 미래를 상징한다.")
                );
                case "NC 다이노스" -> List.of(
                    player(team, "손아섭", "외야수", 31, "NC의 베테랑 중심 타자. 타격왕 출신으로 정교한 배트 컨트롤과 넓은 수비 범위를 자랑하는 경험 많은 리더다."),
                    player(team, "박민우", "내야수", 4, "NC의 리드오프. 빠른 발과 뛰어난 선구안으로 득점 기회를 만들고 경기 흐름을 주도하는 핵심 선수다."),
                    player(team, "김주원", "내야수", 13, "NC의 주전 유격수. 수비와 타격을 겸비한 젊은 핵심 선수로 NC의 미래를 이끌 차세대 스타다."),
                    player(team, "이재학", "투수", 14, "NC의 베테랑 선발 투수. 다양한 구종과 노련한 피칭으로 이닝을 소화하는 팀의 든든한 버팀목이다.")
                );
                case "키움 히어로즈" -> List.of(
                    player(team, "안우진", "투수", 34, "키움의 에이스. KBO 최고 수준의 강속구와 날카로운 슬라이더를 보유한 투수로 키움 투수진의 핵심이다."),
                    player(team, "이주형", "외야수", 27, "키움의 차세대 스타. 빠른 발과 강한 타격이 매력적인 젊은 외야수로 키움의 미래를 책임진다."),
                    player(team, "김혜성", "내야수", 7, "키움의 리드오프이자 주장. 뛰어난 수비와 빠른 발, 탁월한 선구안으로 팀의 공격을 이끄는 핵심 내야수다."),
                    player(team, "하영민", "투수", 11, "키움의 마무리. 강한 직구로 경기를 깔끔하게 마무리하는 팬들이 믿고 보는 마무리 투수다.")
                );
                default -> List.of();
            };

            playerRepository.saveAll(players);
        });
        log.info("선수 데이터 초기화 완료");
    }

    private Player player(Team team, String name, String position, int jerseyNumber, String description) {
        return Player.builder()
                .team(team)
                .name(name)
                .position(position)
                .jerseyNumber(jerseyNumber)
                .description(description)
                .build();
    }
}

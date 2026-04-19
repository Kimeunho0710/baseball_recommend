package com.baseball.recommend.domain.survey;

import com.baseball.recommend.domain.survey.dto.QuestionDto;
import com.baseball.recommend.domain.survey.dto.SurveyRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final ObjectMapper objectMapper;

    public List<QuestionDto> getQuestions() {
        return List.of(
            QuestionDto.builder()
                .id("q1")
                .question("야구장에 처음 갔을 때, 나는?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("응원단석 한가운데로 바로 뛰어들어 응원가를 따라 부른다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("치어리더·선수 잘 보이는 자리에서 경기를 집중해서 본다").build(),
                    QuestionDto.OptionDto.builder().value("C").text("맛있는 거 먹으면서 분위기만 즐긴다").build(),
                    QuestionDto.OptionDto.builder().value("D").text("타율·방어율 앱 켜놓고 데이터 보면서 관전한다").build()
                )).build(),
            QuestionDto.builder()
                .id("q2")
                .question("내 팀이 9회말 2아웃 역전 상황. 나의 반응은?")
                .hint("9회말 2아웃 — 야구에서 가장 극적인 마지막 순간. 한 번만 더 아웃되면 경기 끝!")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("눈물이 핑 돌면서 손이 떨린다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("\"저 타자 성공 확률이 얼마야?\" 냉정하게 따진다").build(),
                    QuestionDto.OptionDto.builder().value("C").text("일단 자리에서 일어나 소리를 지른다").build(),
                    QuestionDto.OptionDto.builder().value("D").text("긴장돼서 못 보고 핸드폰만 들여다본다").build()
                )).build(),
            QuestionDto.builder()
                .id("q3")
                .question("직관 갈 때 같이 가고 싶은 사람은?")
                .hint("직관 — 야구장에 직접 가서 관람하는 것")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("응원가 다 외운 찐팬 친구").build(),
                    QuestionDto.OptionDto.builder().value("B").text("야구 몰라도 분위기 좋아하는 친구").build(),
                    QuestionDto.OptionDto.builder().value("C").text("혼자 조용히 집중해서 보고 싶다").build(),
                    QuestionDto.OptionDto.builder().value("D").text("야구 규칙·전술 설명해줄 수 있는 친구").build()
                )).build(),
            QuestionDto.builder()
                .id("q4")
                .question("응원하는 팀이 10연패 중이다. 나는?")
                .hint("10연패 — 10경기를 연속으로 진 상태. 팬 입장에서 꽤 괴로운 시기")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("그래도 직관 간다. 이럴 때 응원해야 진짜 팬이지").build(),
                    QuestionDto.OptionDto.builder().value("B").text("슬프지만 시즌 포기하고 내년을 기대한다").build(),
                    QuestionDto.OptionDto.builder().value("C").text("감독·프런트 비판글 커뮤니티에 올린다").build(),
                    QuestionDto.OptionDto.builder().value("D").text("그냥 다른 팀 재밌는 경기 본다").build()
                )).build(),
            QuestionDto.builder()
                .id("q5")
                .question("주말 계획을 세울 때 나는?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("월요일에 이미 주말 일정 다 잡아놓는다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("금요일 저녁에 대충 방향만 정한다").build(),
                    QuestionDto.OptionDto.builder().value("C").text("토요일 아침에 일어나서 그때 기분으로 결정").build(),
                    QuestionDto.OptionDto.builder().value("D").text("계획 같은 거 없음. 흘러가는 대로").build()
                )).build(),
            QuestionDto.builder()
                .id("q6")
                .question("새로운 사람을 만났을 때 나는?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("먼저 말 걸고 분위기를 주도한다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("상대가 말 걸면 잘 받아친다").build(),
                    QuestionDto.OptionDto.builder().value("C").text("필요한 말만 하고 조용히 있는다").build(),
                    QuestionDto.OptionDto.builder().value("D").text("일단 관찰하다가 친해지면 많이 떠든다").build()
                )).build(),
            QuestionDto.builder()
                .id("q7")
                .question("게임이나 스포츠에서 나의 스타일은?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("무조건 공격. 실점해도 더 많이 뽑으면 됨").build(),
                    QuestionDto.OptionDto.builder().value("B").text("철벽 수비로 실점 0. 1점이면 이긴다").build(),
                    QuestionDto.OptionDto.builder().value("C").text("화려한 플레이가 최고. 결과보다 과정").build(),
                    QuestionDto.OptionDto.builder().value("D").text("이기는 게 전부. 방법은 상관없다").build()
                )).build(),
            QuestionDto.builder()
                .id("q8")
                .question("나에게 더 끌리는 이야기는?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("수십 년 전통을 이어온 명문의 무게감").build(),
                    QuestionDto.OptionDto.builder().value("B").text("약팀에서 시작해 정상을 밟은 반란의 서사").build(),
                    QuestionDto.OptionDto.builder().value("C").text("스타 한 명이 팀을 통째로 이끄는 영웅담").build(),
                    QuestionDto.OptionDto.builder().value("D").text("조용히 꾸준히 강팀 자리를 지키는 뚝심").build()
                )).build()
        );
    }

    @Transactional
    public SurveyResult saveSurvey(SurveyRequest request) {
        try {
            String answersJson = objectMapper.writeValueAsString(request.getAnswers());
            SurveyResult surveyResult = SurveyResult.builder()
                    .answers(answersJson)
                    .build();
            return surveyRepository.save(surveyResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("설문 답변 저장 중 오류가 발생했습니다.", e);
        }
    }
}

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
                .question("주말에 나는 주로?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("친구들과 활발하게 어울리는 편이다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("혼자 조용히 쉬는 것을 좋아한다").build()
                )).build(),
            QuestionDto.builder()
                .id("q2")
                .question("중요한 결정을 내릴 때 나는?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("논리와 데이터를 기반으로 판단한다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("느낌과 감정을 더 중요시한다").build()
                )).build(),
            QuestionDto.builder()
                .id("q3")
                .question("나의 생활 방식은?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("계획을 세우고 체계적으로 실행한다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("그때그때 상황에 맞게 유연하게 대처한다").build()
                )).build(),
            QuestionDto.builder()
                .id("q4")
                .question("새로운 도전에 대해?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("설레고 적극적으로 뛰어든다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("신중하게 검토하고 안정을 추구한다").build()
                )).build(),
            QuestionDto.builder()
                .id("q5")
                .question("경기 관람 스타일은?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("열정적으로 응원하며 분위기를 즐긴다").build(),
                    QuestionDto.OptionDto.builder().value("B").text("조용히 집중해서 경기를 분석하며 본다").build()
                )).build(),
            QuestionDto.builder()
                .id("q6")
                .question("어떤 스토리가 더 감동적인가?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("극적인 역전승, 포기하지 않는 투지").build(),
                    QuestionDto.OptionDto.builder().value("B").text("처음부터 끝까지 완벽한 경기 운영").build()
                )).build(),
            QuestionDto.builder()
                .id("q7")
                .question("야구 관람의 목적은?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("현장 분위기와 재미를 즐기기 위해").build(),
                    QuestionDto.OptionDto.builder().value("B").text("좋아하는 팀의 승리를 위해 진지하게").build()
                )).build(),
            QuestionDto.builder()
                .id("q8")
                .question("나는 어떤 스타일의 사람인가?")
                .options(List.of(
                    QuestionDto.OptionDto.builder().value("A").text("화끈하고 감성적인 스타일").build(),
                    QuestionDto.OptionDto.builder().value("B").text("차분하고 논리적인 스타일").build()
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

package com.portfolio.memo.chat.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatMessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    @DisplayName("메시지를 저장하고 ID로 조회하면 정상적으로 찾아져야 한다")
    void saveAndFindById_shouldWorkCorrectly() {
        // Given (설정)
        // TestEntityManager를 사용하여 엔티티를 영속성 컨텍스트에 저장 준비
        ChatMessage newMessage = ChatMessage.builder()
                .roomId(1L)
                .senderId(100L)
                .senderName("testUser")
                .content("Hello, JPA Test!")
                .build();

        // When (실행)
        // 실제 리포지토리의 save 메서드를 호출하여 DB에 저장
        ChatMessage savedMessage = chatMessageRepository.save(newMessage);
        
        // 영속성 컨텍스트를 flush하여 DB에 즉시 반영하고, clear하여 1차 캐시를 비움
        // 이렇게 하면 findById가 캐시가 아닌 DB에서 데이터를 가져오도록 보장
        entityManager.flush();
        entityManager.clear();

        // 저장된 메시지를 ID로 다시 조회
        Optional<ChatMessage> foundMessageOpt = chatMessageRepository.findById(savedMessage.getId());

        // Then (검증)
        assertThat(foundMessageOpt).isPresent(); // 조회된 결과가 존재해야 함
        ChatMessage foundMessage = foundMessageOpt.get();

        // 저장된 내용과 조회된 내용이 일치하는지 확인
        assertThat(foundMessage.getId()).isEqualTo(savedMessage.getId());
        assertThat(foundMessage.getRoomId()).isEqualTo(newMessage.getRoomId());
        assertThat(foundMessage.getSenderId()).isEqualTo(newMessage.getSenderId());
        assertThat(foundMessage.getContent()).isEqualTo(newMessage.getContent());
    }
}

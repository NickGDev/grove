package ie.grove.messaging;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByConversationId(Long conversationId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);
}

package chat.com.test.chat;

import chat.com.test.repository.User;
import chat.com.test.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage){
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor){
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }
    // 로그인 요청 처리

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    // 로그인 요청 처리
    @MessageMapping("/chat.login")
    public void loginUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String password = chatMessage.getContent();

        // 로그인 로직
        User user = userRepository.findByUsernameAndPassword(username, password);
        if (user != null) {
            // 로그인 성공 시 세션에 사용자 정보 저장
            headerAccessor.getSessionAttributes().put("username", username);

            // 클라이언트에게 로그인 성공 여부 메시지 전송
            messagingTemplate.convertAndSendToUser(username, "/topic/public", ChatMessage.builder()
                    .content("Login success")
                    .type(MessageType.LOGIN)
                    .sender(username)
                    .build());
        } else {
            // 로그인 실패 시 클라이언트에게 실패 메시지 전송
            messagingTemplate.convertAndSendToUser(username, "/topic/public", ChatMessage.builder()
                    .content("Login failed")
                    .type(MessageType.ERROR)
                    .sender(username)
                    .build());
        }
    }

    // 회원가입 요청 처리
    @MessageMapping("/chat.signup")
    public void signupUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String password = chatMessage.getContent();

        // 회원가입 로직
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            // 이미 존재하는 사용자인 경우
            messagingTemplate.convertAndSendToUser(username, "/topic/public", ChatMessage.builder()
                    .content("Username already exists")
                    .type(MessageType.ERROR)
                    .sender(username)
                    .build());
        } else {
            // 새로운 사용자인 경우, 회원가입 성공
            User newUser = new User(username, password);
            userRepository.save(newUser);

            // 클라이언트에게 회원가입 성공 여부 메시지 전송
            messagingTemplate.convertAndSendToUser(username, "/topic/public", ChatMessage.builder()
                    .content("Signup success")
                    .type(MessageType.SIGNUP)
                    .sender(username)
                    .build());
        }
    }


}

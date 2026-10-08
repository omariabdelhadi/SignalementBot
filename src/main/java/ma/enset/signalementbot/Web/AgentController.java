package ma.enset.signalementbot.Web;


import ma.enset.signalementbot.Agent.AIagent;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chatbot")
public class AgentController {
    @Autowired
    private AIagent aIagent;

    @PostMapping("/demande")
    public ResponseEntity<String> demande(
            @RequestParam String question,
            @RequestParam(required=false) MultipartFile image,
            @RequestParam String id,
            @CookieValue(value = "JSESSIONID", required = false) String sessionId) {

        // Vérifier la session auprès de backend-signalement
        RestClient restClient = RestClient.create();
        String role;
        try {
            var response=restClient.get()
                    .uri("http://localhost:8222/api/signalement/check-session")
                    .header("Cookie", "JSESSIONID=" + sessionId)
                    .retrieve()
                    .toEntity(String.class);
            String boy=response.getBody();
            role= boy.contains("ADMIN")?"ADMIN":"USER";
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Non authentifié");
        }
        return ResponseEntity.ok(aIagent.AskAgent(question,image,role,id,sessionId));
    }
    @DeleteMapping("/SupprimerMemory/{id}")
    public void deleteMemory(@PathVariable String id){
        aIagent.viderMemory(id);
    }
}

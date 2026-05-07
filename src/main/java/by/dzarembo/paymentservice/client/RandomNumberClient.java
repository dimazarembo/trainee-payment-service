package by.dzarembo.paymentservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RandomNumberClient {
    private final RestClient restClient;

    public int getRandomNumber() {
        String number = restClient.get()
                .uri("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new")
                .retrieve()
                .body(String.class);

        if (number == null || number.isBlank()) {
            throw new IllegalStateException("Random number API returned empty response");
        }

        return Integer.parseInt(number.trim());
    }
}

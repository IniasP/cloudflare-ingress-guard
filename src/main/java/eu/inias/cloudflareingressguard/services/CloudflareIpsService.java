package eu.inias.cloudflareingressguard.services;

import eu.inias.cloudflareingressguard.util.CachedValue;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CloudflareIpsService {
    private final RestClient restClient;
    private final CachedValue<List<String>> cachedIps;

    public CloudflareIpsService(RestClient restClient) {
        this.restClient = restClient;
        this.cachedIps = CachedValue.lazy(this::getCloudflareIps, Duration.ofDays(7));
    }

    public List<String> getCachedCloudflareIps() {
        return cachedIps.get();
    }

    private List<String> getCloudflareIps() {
        Stream<String> ipv4s = restClient.get()
                .uri("https://www.cloudflare.com/ips-v4")
                .retrieve()
                .body(String.class)
                .lines()
                .map(String::strip);
        Stream<String> ipv6s = restClient.get()
                .uri("https://www.cloudflare.com/ips-v6")
                .retrieve()
                .body(String.class)
                .lines()
                .map(String::strip);
        return Stream.concat(ipv4s, ipv6s).toList();
    }
}

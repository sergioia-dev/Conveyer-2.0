package conveyer.backend.configuration.service;

import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

@Service
public class RateLimitingService {

  private static final int REQUEST_PER_MINUTE = 10;

  private final ProxyManager<String> proxyManager;

  public RateLimitingService(ProxyManager<String> proxyManager) {
    this.proxyManager = proxyManager;
  }

  public Bucket resolveBucket(String key) {
    Supplier<BucketConfiguration> configSupplier = this::getConfig;

    return proxyManager.builder().build(key, configSupplier);

  }

  private BucketConfiguration getConfig() {
    var limit = Bandwidth.builder().capacity(REQUEST_PER_MINUTE)
        .refillIntervally(REQUEST_PER_MINUTE, Duration.ofMinutes(1))
        .build();

    return BucketConfiguration.builder().addLimit(limit).build();
  }

}

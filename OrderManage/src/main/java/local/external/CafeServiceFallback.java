package local.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Component
public class CafeServiceFallback implements CafeService {

    @Override
    public Cafe getCafeStatus(Long cafeId){
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
        return null;
    }
}
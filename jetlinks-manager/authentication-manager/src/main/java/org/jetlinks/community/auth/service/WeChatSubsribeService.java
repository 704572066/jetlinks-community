package org.jetlinks.community.auth.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.WeChatSubscribeEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class WeChatSubsribeService extends GenericReactiveCrudService<WeChatSubscribeEntity, String> {

    public Mono<WeChatSubscribeEntity> findByUnionId(String unionId) {
        return createQuery()
            .where(WeChatSubscribeEntity::getUinonid, unionId)
            .fetchOne();
    }


}

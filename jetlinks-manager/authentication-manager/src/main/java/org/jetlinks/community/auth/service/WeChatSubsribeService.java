package org.jetlinks.community.auth.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.WeChatSubscribeEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class WeChatSubsribeService extends GenericReactiveCrudService<WeChatSubscribeEntity, String> {

    public Mono<WeChatSubscribeEntity> findByUnionId(String unionId) {
        return createQuery()
            .where(WeChatSubscribeEntity::getUinonid, unionId)
            .fetchOne();
    }

    public Mono<List<String>> findOpenidListByUnionId(List<String> unionids) {
        return createQuery()
//            .where(WeChatSubscribeEntity::getUinonid, unionId)
            .where(WeChatSubscribeEntity::getStatus, 1).in(WeChatSubscribeEntity::getUinonid, unionids)
            .fetch()
            .map(WeChatSubscribeEntity::getId)
            .collectList();
    }


}

package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.dto.BulkDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor
public class BulkService {
    private final RequestService requests;
    private final PlatformTransactionManager transactions;
    public Response apply(Input input) {
        if (input.operation() == Operation.SET_STATUS && input.requestStatus() == null) throw SearchService.bad("requestStatus is required");
        if ((input.operation() == Operation.ADD_TAG || input.operation() == Operation.REMOVE_TAG) && input.tagId() == null) throw SearchService.bad("tagId is required");
        var seen = new HashSet<UUID>(); var results = new ArrayList<Result>(); var tx = new TransactionTemplate(transactions);
        for (Item item : input.items()) {
            if (!seen.add(item.id())) { results.add(new Result(item.id(), false, 400, "Duplicate UUID in bulk operation", null)); continue; }
            try { results.add(new Result(item.id(), true, 200, null, tx.execute(status -> requests.bulk(item, input)))); }
            catch (ResponseStatusException ex) { results.add(new Result(item.id(), false, ex.getStatusCode().value(), ex.getReason(), null)); }
            catch (org.springframework.dao.OptimisticLockingFailureException | org.springframework.dao.DataIntegrityViolationException ex) { results.add(new Result(item.id(), false, 409, "Concurrent update or constraint conflict", null)); }
        }
        int succeeded = (int) results.stream().filter(Result::success).count();
        return new Response(succeeded, results.size() - succeeded, results);
    }
}

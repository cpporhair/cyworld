package com.cyworld.coordinator.transactions;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class transaction {
    String uuid;
    int status;
}

package uk.gov.hmcts.marketplace.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyResponse {

    /** Number of subscribers notified successfully */
    private int notified;
}

package com.fundmetrics.api.model.chooser;

import com.fundmetrics.api.model.FooterNote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Shaped response for the Active Series fund chooser page.
 *
 * <p>Each fund's metrics are self-contained — labels, descriptions, and values travel
 * together so the micro frontend can render each card without any cross-referencing or
 * client-side joins against a separate {@code metricDescriptions} map.
 */
@Schema(description = "Fund chooser page response — metrics are self-contained with display strings co-located")
@Value
@Builder
public class FundChooserResponse {

    @Schema(description = "Short legal disclaimer shown inline with fund data")
    String disclaimer;

    @Schema(description = "Ordered list of keyed disclaimer paragraphs shown in the chooser page footer")
    List<FooterNote> footerNotes;

    @Schema(description = "Date up to which performance figures are calculated", example = "2025-03-31")
    String performanceAsOf;

    @Schema(description = "Ordered list of funds to display as chooser cards")
    List<FundChooserItem> funds;
}

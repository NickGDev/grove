package ie.grove.billing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillingPageControllerTests {

  @Autowired MockMvc mvc;

  @Test
  @WithMockUser(roles = "OWNER")
  void feesPageRendersWithSeedRows() throws Exception {
    mvc.perform(get("/admin/fees"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Easy fees")))
        .andExpect(content().string(containsString("Total due")))
        .andExpect(content().string(containsString("Maria Jones")))
        .andExpect(content().string(containsString("Record payment")));
  }

  @Test
  @WithMockUser(roles = "OWNER")
  void weekFragmentRendersRequestedWeekAndSort() throws Exception {
    mvc.perform(get("/admin/fees/panel").param("week", "2026-W40").param("sort", "desc"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Week 40")));
  }

  @Test
  @WithMockUser(roles = "OWNER")
  void recordPaymentMarksRowPaidAndTriggersToast() throws Exception {
    mvc.perform(post("/admin/fees/2/pay").param("week", "2026-W40").param("sort", "asc").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(header().string("HX-Trigger", "payment-recorded"))
        .andExpect(content().string(containsString("Paid in full")));
  }
}

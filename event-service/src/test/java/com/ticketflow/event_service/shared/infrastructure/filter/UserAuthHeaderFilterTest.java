package com.ticketflow.event_service.shared.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthHeaderFilter (event-service) — unit tests")
class UserAuthHeaderFilterTest {

    private static final String INTERNAL_KEY = "test-internal-secret";

    @Mock
    private FilterChain filterChain;

    private UserAuthHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserAuthHeaderFilter(new ObjectMapper(), INTERNAL_KEY);
    }

    // -------------------------------------------------------------------------
    // Actuator paths — always allowed
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Actuator paths")
    class Actuator {

        @Test
        @DisplayName("should pass through without any header")
        void actuator_noHeader_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/actuator/health");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
    }

    // -------------------------------------------------------------------------
    // Internal paths — require X-Internal-Api-Key
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Internal paths")
    class Internal {

        @Test
        @DisplayName("should pass through with correct internal API key on decrement-tickets")
        void decrement_validKey_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("PATCH", "/api/v1/events/EVT-001/decrement-tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-Internal-Api-Key", INTERNAL_KEY);

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("should pass through with correct internal API key on increment-tickets")
        void increment_validKey_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("PATCH", "/api/v1/events/EVT-001/increment-tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-Internal-Api-Key", INTERNAL_KEY);

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("should pass through with correct internal API key on my-event-ids")
        void myEventIds_validKey_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/api/v1/events/my-event-ids");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-Internal-Api-Key", INTERNAL_KEY);

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("should return 401 when internal API key is missing")
        void internalPath_missingKey_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("PATCH", "/api/v1/events/EVT-001/decrement-tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(resp.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("should return 401 when internal API key is wrong")
        void internalPath_wrongKey_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("PATCH", "/api/v1/events/EVT-001/decrement-tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-Internal-Api-Key", "wrong-key");

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    // -------------------------------------------------------------------------
    // Public GET paths — no auth required
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Public read paths")
    class PublicReads {

        @Test
        @DisplayName("GET /api/v1/events passes without any header")
        void getAll_noHeader_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/api/v1/events");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("GET /api/v1/events/{id} passes without any header")
        void getById_noHeader_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/api/v1/events/EVT-001");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }
    }

    // -------------------------------------------------------------------------
    // Protected paths — require X-User-Id
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Protected paths")
    class Protected {

        @Test
        @DisplayName("POST /api/v1/events with X-User-Id passes")
        void create_withUserId_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("POST", "/api/v1/events");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-User-Id", "user-001");

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("POST /api/v1/events without X-User-Id returns 401")
        void create_missingUserId_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("POST", "/api/v1/events");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("DELETE /api/v1/events/{id} without X-User-Id returns 401")
        void delete_missingUserId_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("DELETE", "/api/v1/events/EVT-001");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }
}

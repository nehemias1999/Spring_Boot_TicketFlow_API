package com.ticketflow.ticket_service.shared.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
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
@DisplayName("UserAuthHeaderFilter (ticket-service) — unit tests")
class UserAuthHeaderFilterTest {

    @Mock
    private FilterChain filterChain;

    private UserAuthHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserAuthHeaderFilter(new ObjectMapper());
    }

    @Nested
    @DisplayName("Actuator paths")
    class Actuator {

        @Test
        @DisplayName("should pass through /actuator/health without any header")
        void actuator_noHeader_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/actuator/health");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
    }

    @Nested
    @DisplayName("Protected ticket endpoints")
    class Protected {

        @Test
        @DisplayName("POST /api/v1/tickets with X-User-Id passes")
        void create_withUserId_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("POST", "/api/v1/tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-User-Id", "user-001");

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("POST /api/v1/tickets without X-User-Id returns 401")
        void create_missingUserId_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("POST", "/api/v1/tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(resp.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("GET /api/v1/tickets without X-User-Id returns 401")
        void getAll_missingUserId_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/api/v1/tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("GET /api/v1/tickets/{id} with X-User-Id passes")
        void getById_withUserId_passes() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/api/v1/tickets/TKT-001");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-User-Id", "user-001");

            filter.doFilter(req, resp, filterChain);

            verify(filterChain).doFilter(req, resp);
        }

        @Test
        @DisplayName("PATCH /api/v1/tickets/{id}/cancel without X-User-Id returns 401")
        void cancel_missingUserId_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("PATCH", "/api/v1/tickets/TKT-001/cancel");
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("blank X-User-Id returns 401")
        void blankUserId_returns401() throws Exception {
            MockHttpServletRequest  req  = new MockHttpServletRequest("GET", "/api/v1/tickets");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            req.addHeader("X-User-Id", "   ");

            filter.doFilter(req, resp, filterChain);

            verify(filterChain, never()).doFilter(req, resp);
            assertThat(resp.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }
}

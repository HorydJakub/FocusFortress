package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.RegistrationPayload;
import com.focusfortress.api.support.TestUser;
import com.focusfortress.dto.VideoTileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.focusfortress.api.support.Payloads.mediaItem;
import static com.focusfortress.api.support.Payloads.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Media API  /api/media")
class MediaApiTest extends ApiTestBase {

    private TestUser alice;

    @BeforeEach
    void setUp() {
        alice = auth.registerVerifiedUser();
    }

    @Nested
    @DisplayName("library  /api/media/library")
    class Library {

        @Test
        @DisplayName("POST adds a video with default status WATCH_LATER")
        void addsVideo() {
            String videoId = unique("vid");

            as(alice).body(mediaItem(videoId))
                    .when().post("/media/library")
                    .then()
                    .statusCode(201)
                    .header("Location", notNullValue())
                    .body("id", notNullValue())
                    .body("videoId", equalTo(videoId))
                    .body("title", equalTo("Stoicism explained in 10 minutes"))
                    .body("status", equalTo("WATCH_LATER"))
                    .body("addedAt", notNullValue());
        }

        @Test
        @DisplayName("POST rejects missing videoId and title with 400")
        void rejectsInvalidItem() {
            as(alice).body(Map.of("channelTitle", "No id, no title"))
                    .when().post("/media/library")
                    .then()
                    .statusCode(400)
                    .body("errors", hasKey("videoId"))
                    .body("errors", hasKey("title"));
        }

        @Test
        @DisplayName("POST rejects the same video twice with 409")
        void rejectsDuplicateVideo() {
            String videoId = unique("vid");
            createId(alice, "/media/library", mediaItem(videoId));

            as(alice).body(mediaItem(videoId))
                    .when().post("/media/library")
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("This video is already in your library"));
        }

        @Test
        @DisplayName("GET filters by status")
        void filtersByStatus() {
            long watching = createId(alice, "/media/library", withStatus(mediaItem(unique("vid")), "CURRENTLY_WATCHING"));
            long later = createId(alice, "/media/library", mediaItem(unique("vid")));

            as(alice).get("/media/library")
                    .then().statusCode(200).body("id", containsInAnyOrder((int) watching, (int) later));
            as(alice).queryParam("status", "CURRENTLY_WATCHING").get("/media/library")
                    .then().statusCode(200).body("id", contains((int) watching));
        }

        @Test
        @DisplayName("PATCH /{id}/status changes status and notes")
        void updatesStatus() {
            long id = createId(alice, "/media/library", mediaItem(unique("vid")));

            as(alice).body(Map.of("status", "FINISHED", "notes", "Great intro"))
                    .when().patch("/media/library/{id}/status", id)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("FINISHED"))
                    .body("notes", equalTo("Great intro"));
        }

        @Test
        @DisplayName("PATCH /{id}/status requires a status (400)")
        void statusIsRequired() {
            long id = createId(alice, "/media/library", mediaItem(unique("vid")));

            as(alice).body(Map.of("notes", "no status"))
                    .when().patch("/media/library/{id}/status", id)
                    .then()
                    .statusCode(400)
                    .body("errors", hasKey("status"));
        }

        @Test
        @DisplayName("DELETE /{id} removes the video")
        void deletesVideo() {
            long id = createId(alice, "/media/library", mediaItem(unique("vid")));

            as(alice).delete("/media/library/{id}", id).then().statusCode(204);
            as(alice).get("/media/library").then().body("id", not(hasItem((int) id)));
        }

        @Test
        @DisplayName("GET with an unknown status value returns 400")
        void rejectsUnknownStatusFilter() {
            as(alice).queryParam("status", "NOPE").get("/media/library").then().statusCode(400);
        }

        @Nested
        @DisplayName("user isolation")
        class Isolation {

            private TestUser bob;
            private long aliceItemId;

            @BeforeEach
            void aliceHasAVideo() {
                bob = auth.registerVerifiedUser();
                aliceItemId = createId(alice, "/media/library", mediaItem(unique("vid")));
            }

            @Test
            @DisplayName("another user's library does not contain it")
            void notListed() {
                as(bob).get("/media/library").then().statusCode(200).body("$", hasSize(0));
            }

            @Test
            @DisplayName("another user cannot change its status (404)")
            void cannotUpdate() {
                as(bob).body(Map.of("status", "FINISHED"))
                        .when().patch("/media/library/{id}/status", aliceItemId)
                        .then()
                        .statusCode(404);

                as(alice).get("/media/library").then().body("[0].status", equalTo("WATCH_LATER"));
            }

            @Test
            @DisplayName("another user cannot delete it (404)")
            void cannotDelete() {
                as(bob).delete("/media/library/{id}", aliceItemId).then().statusCode(404);

                as(alice).get("/media/library").then().body("id", hasItem((int) aliceItemId));
            }

            @Test
            @DisplayName("another user can add the same video to their own library")
            void sameVideoForAnotherUser() {
                String videoId = as(alice).get("/media/library").path("[0].videoId");

                as(bob).body(mediaItem(videoId)).post("/media/library").then().statusCode(201);
            }
        }

        private Map<String, Object> withStatus(Map<String, Object> body, String status) {
            body.put("status", status);
            return body;
        }
    }

    @Nested
    @DisplayName("tiles  /api/media/tiles  (YouTube mocked)")
    class Tiles {

        private static final List<String> INTERESTS = List.of("Meditation", "Physics", "Reading");

        @BeforeEach
        void youTubeReturnsFakeVideos() {
            // fresh DTOs per call: the service mutates matchedInterest on what the client returns
            when(youTubeApiClient.searchEducationalVideos(anyString(), anyInt()))
                    .thenAnswer(call -> fakeVideos(call.getArgument(0), call.getArgument(1)));
        }

        @Test
        @DisplayName("returns tiles tagged with the user's interests, one YouTube query per interest")
        void personalizedTiles() {
            as(alice).queryParam("limit", 6).get("/media/tiles")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(6))
                    .body("videoId", everyItem(notNullValue()))
                    .body("matchedInterest", everyItem(in(INTERESTS)));

            verify(youTubeApiClient, times(INTERESTS.size())).searchEducationalVideos(anyString(), anyInt());
        }

        @Test
        @DisplayName("default limit is 12")
        void defaultLimit() {
            as(alice).get("/media/tiles").then().statusCode(200).body("$", hasSize(12));
        }

        @Test
        @DisplayName("user without interests gets an empty list and YouTube is not called")
        void noInterestsNoTiles() {
            TestUser bob = auth.registerVerifiedUser(RegistrationPayload.aValidRegistration()
                    .with("selectedInterests", INTERESTS));
            as(bob).body(Map.of("add", List.of(), "remove", INTERESTS)).put("/user/interests").then().statusCode(200);

            as(bob).get("/media/tiles").then().statusCode(200).body("$", hasSize(0));

            verify(youTubeApiClient, never()).searchEducationalVideos(anyString(), anyInt());
        }

        @Test
        @DisplayName("/custom only queries the requested interests the user actually has")
        void customTilesForOwnInterest() {
            as(alice).queryParam("subcategories", "Physics,Astronomy")
                    .get("/media/tiles/custom")
                    .then()
                    .statusCode(200)
                    .body("matchedInterest", everyItem(equalTo("Physics")));

            verify(youTubeApiClient, times(1)).searchEducationalVideos(ArgumentMatchers.contains("Physics"), anyInt());
            verify(youTubeApiClient, never()).searchEducationalVideos(ArgumentMatchers.contains("Astronomy"), anyInt());
        }

        @Test
        @DisplayName("/custom with interests the user does not have returns [] without calling YouTube")
        void customTilesForForeignInterest() {
            as(alice).queryParam("subcategories", "Astronomy")
                    .get("/media/tiles/custom")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(0));

            verify(youTubeApiClient, never()).searchEducationalVideos(anyString(), anyInt());
        }

        @Test
        @DisplayName("custom interests are searched too")
        void customInterestIsSearched() {
            String custom = unique("Origami");
            as(alice).body(Map.of("name", custom, "emoji", "🦢")).post("/user/interests/custom").then().statusCode(200);

            as(alice).queryParam("subcategories", custom)
                    .get("/media/tiles/custom")
                    .then()
                    .statusCode(200)
                    .body("matchedInterest", everyItem(equalTo(custom)))
                    .body("$", not(hasSize(0)));
        }

        @Test
        @DisplayName("YouTube outage (empty result) degrades to an empty list, not an error")
        void youTubeReturnsNothing() {
            when(youTubeApiClient.searchEducationalVideos(anyString(), anyInt())).thenReturn(List.of());

            as(alice).get("/media/tiles").then().statusCode(200).body("$", hasSize(0));
        }

        private List<VideoTileDTO> fakeVideos(String query, int count) {
            return IntStream.range(0, count)
                    .mapToObj(i -> new VideoTileDTO(
                            unique("yt"),
                            query + " #" + i,
                            "Fake Channel",
                            "https://img.example.test/" + i + ".jpg",
                            "Fake result for " + query,
                            null))
                    .toList();
        }
    }
}

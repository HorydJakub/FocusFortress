package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.focusfortress.api.support.Payloads.category;
import static com.focusfortress.api.support.Payloads.subcategory;
import static com.focusfortress.api.support.Payloads.unique;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Categories API  /api/categories, /api/subcategories")
class CategoryApiTest extends ApiTestBase {

    private TestUser user;

    @BeforeEach
    void setUp() {
        user = auth.registerVerifiedUser();
    }

    @Nested
    @DisplayName("categories")
    class Categories {

        @Test
        @DisplayName("a new user starts without habit categories (interest catalog is not listed)")
        void newUserHasNoCategories() {
            // default registration picks Meditation, Physics and Reading
            as(user).get("/categories")
                    .then()
                    .statusCode(200)
                    .body("name", not(hasItems("Mind & Body", "Natural Sciences", "Skills & Learning")))
                    .body("size()", equalTo(0));
        }

        @Test
        @DisplayName("two users can each have a category with the same name")
        void categoryNamesArePerUser() {
            String name = unique("Fitness");
            as(user).body(category(name)).post("/categories").then().statusCode(201);

            as(auth.registerVerifiedUser()).body(category(name)).post("/categories").then().statusCode(201);
        }

        @Test
        @DisplayName("another user's category is invisible in the list and by id")
        void categoriesAreIsolated() {
            long id = createCategory(user);
            TestUser other = auth.registerVerifiedUser();

            as(other).get("/categories").then().body("id", not(hasItem((int) id)));
            as(other).get("/categories/{id}", id).then().statusCode(404);
            as(other).body(category(unique("Hijack"))).put("/categories/{id}", id).then().statusCode(404);
        }

        @Test
        @DisplayName("POST creates a category, GET /{id} returns it")
        void createsAndReadsCategory() {
            String name = unique("Languages");

            int id = as(user).body(category(name))
                    .when().post("/categories")
                    .then()
                    .statusCode(201)
                    .header("Location", notNullValue())
                    .body("name", equalTo(name))
                    .body("icon", equalTo("🧪"))
                    .extract().path("id");

            as(user).get("/categories/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("name", equalTo(name));
        }

        @Test
        @DisplayName("POST rejects blank name and oversized icon with 400")
        void rejectsInvalidCategory() {
            as(user).body(Map.of("name", "", "icon", "TOOLONG"))
                    .when().post("/categories")
                    .then()
                    .statusCode(400)
                    .body("errors", hasKey("name"))
                    .body("errors", hasKey("icon"));
        }

        @Test
        @DisplayName("POST rejects duplicate name with 409")
        void rejectsDuplicateCategory() {
            String name = unique("Duplicate");
            createId(user, "/categories", category(name));

            as(user).body(category(name))
                    .when().post("/categories")
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("Category with this name already exists"));
        }

        @Test
        @DisplayName("PUT renames a category")
        void updatesCategory() {
            long id = createCategory(user);
            String newName = unique("Renamed");

            as(user).body(category(newName))
                    .when().put("/categories/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo(newName));
        }

        @Test
        @DisplayName("DELETE removes a category")
        void deletesCategory() {
            long id = createCategory(user);

            as(user).delete("/categories/{id}", id).then().statusCode(204);

            as(user).get("/categories/{id}", id).then().statusCode(404);
            as(user).get("/categories").then().body("id", not(hasItem((int) id)));
        }

        @Test
        @DisplayName("unknown id returns 404 for GET, PUT and DELETE")
        void unknownCategory() {
            as(user).get("/categories/{id}", Long.MAX_VALUE)
                    .then().statusCode(404).body("message", equalTo("Category not found"));
            as(user).body(category(unique("X"))).put("/categories/{id}", Long.MAX_VALUE)
                    .then().statusCode(404);
            as(user).delete("/categories/{id}", Long.MAX_VALUE)
                    .then().statusCode(404);
        }

        @Test
        @DisplayName("another user cannot delete someone else's category")
        void otherUserCannotDeleteCategory() {
            long categoryId = createCategory(user);

            as(auth.registerVerifiedUser()).delete("/categories/{id}", categoryId)
                    .then()
                    .statusCode(404);
            as(user).get("/categories/{id}", categoryId).then().statusCode(200);
        }
    }

    @Nested
    @DisplayName("subcategories")
    class Subcategories {

        private long categoryId;

        @BeforeEach
        void createParentCategory() {
            categoryId = createCategory(user);
        }

        @Test
        @DisplayName("POST creates a subcategory linked to its category")
        void createsSubcategory() {
            String name = unique("Spanish");

            int id = as(user).body(subcategory(name, categoryId))
                    .when().post("/subcategories")
                    .then()
                    .statusCode(201)
                    .body("name", equalTo(name))
                    .body("categoryId", equalTo((int) categoryId))
                    .extract().path("id");

            as(user).get("/subcategories/{id}", id).then().statusCode(200).body("name", equalTo(name));
        }

        @Test
        @DisplayName("POST requires categoryId (400)")
        void requiresCategoryId() {
            as(user).body(subcategory(unique("Orphan"), null))
                    .when().post("/subcategories")
                    .then()
                    .statusCode(400)
                    .body("errors", hasKey("categoryId"));
        }

        @Test
        @DisplayName("POST with unknown category returns 404")
        void unknownParentCategory() {
            as(user).body(subcategory(unique("Orphan"), Long.MAX_VALUE))
                    .when().post("/subcategories")
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Category not found"));
        }

        @Test
        @DisplayName("POST rejects duplicate name within the same category (409), allows it in another")
        void duplicateNameScopedToCategory() {
            String name = unique("Shared");
            createId(user, "/subcategories", subcategory(name, categoryId));

            as(user).body(subcategory(name, categoryId)).post("/subcategories").then().statusCode(409);
            as(user).body(subcategory(name, createCategory(user))).post("/subcategories").then().statusCode(201);
        }

        @Test
        @DisplayName("PUT can move a subcategory to another category")
        void movesSubcategory() {
            long id = createSubcategory(user, categoryId);
            long target = createCategory(user);
            String name = unique("Moved");

            as(user).body(subcategory(name, target))
                    .when().put("/subcategories/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo(name))
                    .body("categoryId", equalTo((int) target));
        }

        @Test
        @DisplayName("DELETE removes a subcategory")
        void deletesSubcategory() {
            long id = createSubcategory(user, categoryId);

            as(user).delete("/subcategories/{id}", id).then().statusCode(204);
            as(user).get("/subcategories/{id}", id)
                    .then().statusCode(404).body("message", equalTo("Subcategory not found"));
        }
    }
}

package io.github.salomax.procureflow.app.test.catalog

import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import io.github.salomax.procureflow.app.catalog.service.CatalogItemService
import io.github.salomax.procureflow.common.test.integration.BaseIntegrationTest
import io.github.salomax.procureflow.common.test.integration.PostgresIntegrationTest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.UUID

@MicronautTest(startApplication = true)
@DisplayName("CatalogItem Service Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@Tag("catalog")
@TestMethodOrder(MethodOrderer.Random::class)
class CatalogItemServiceIntegrationTest : BaseIntegrationTest(), PostgresIntegrationTest {

    @Inject
    lateinit var catalogItemService: CatalogItemService

    private fun uniqueName() = "Test Item ${System.currentTimeMillis()}-${Thread.currentThread().id}"

    @Test
    fun `should search catalog items by name`() {
        // Arrange - create test items
        val item1 = CatalogItem(
            name = "USB-C Cable - 1m",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 1500L,
            status = CatalogItemStatus.ACTIVE
        )
        val item2 = CatalogItem(
            name = "USB-C Adapter",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 2000L,
            status = CatalogItemStatus.ACTIVE
        )
        val item3 = CatalogItem(
            name = "Consulting Service",
            category = CatalogItemCategory.SERVICE,
            priceCents = 150000L,
            status = CatalogItemStatus.ACTIVE
        )

        catalogItemService.create(item1)
        catalogItemService.create(item2)
        catalogItemService.create(item3)

        // Act - search for "USB-C"
        val results = catalogItemService.search("USB-C")

        // Assert
        assertThat(results).hasSize(2)
        assertThat(results.map { it.name }).containsExactlyInAnyOrder("USB-C Cable - 1m", "USB-C Adapter")
    }

    @Test
    fun `should return all items when search query is blank`() {
        // Arrange - create multiple test items
        val item1 = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.MATERIAL,
            priceCents = 1500L,
            status = CatalogItemStatus.ACTIVE
        )
        val item2 = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.SERVICE,
            priceCents = 2000L,
            status = CatalogItemStatus.ACTIVE
        )
        val item3 = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.MATERIAL,
            priceCents = 3000L,
            status = CatalogItemStatus.PENDING_APPROVAL
        )

        val created1 = catalogItemService.create(item1)
        val created2 = catalogItemService.create(item2)
        val created3 = catalogItemService.create(item3)

        // Act - search with blank query
        val results = catalogItemService.search("")

        // Assert - should return all items
        assertThat(results).hasSizeGreaterThanOrEqualTo(3)
        val resultIds = results.map { it.id }.toSet()
        assertThat(resultIds).contains(created1.id, created2.id, created3.id)
    }

    @Test
    fun `should return all items when search query is whitespace only`() {
        // Arrange - create test items
        val item1 = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.MATERIAL,
            priceCents = 1500L,
            status = CatalogItemStatus.ACTIVE
        )
        val item2 = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.SERVICE,
            priceCents = 2000L,
            status = CatalogItemStatus.ACTIVE
        )

        val created1 = catalogItemService.create(item1)
        val created2 = catalogItemService.create(item2)

        // Act - search with whitespace-only query
        val results = catalogItemService.search("   ")

        // Assert - should return all items (whitespace is treated as blank)
        assertThat(results).hasSizeGreaterThanOrEqualTo(2)
        val resultIds = results.map { it.id }.toSet()
        assertThat(resultIds).contains(created1.id, created2.id)
    }

    @Test
    fun `should return empty list when no items match search`() {
        // Act
        val results = catalogItemService.search("NonExistentItem")

        // Assert
        assertThat(results).isEmpty()
    }

    @Test
    fun `should search catalog items by description`() {
        // Arrange - create test items with descriptions
        val item1 = CatalogItem(
            name = "Screwdriver Set",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 5000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Professional screwdriver set with multiple sizes"
        )
        val item2 = CatalogItem(
            name = "Hammer",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 3000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Heavy-duty hammer for construction work"
        )
        val item3 = CatalogItem(
            name = "Wrench",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 4000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Adjustable wrench for plumbing"
        )

        catalogItemService.create(item1)
        catalogItemService.create(item2)
        catalogItemService.create(item3)

        // Act - search for "construction" which is in description
        val results = catalogItemService.search("construction")

        // Assert
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("Hammer")
        assertThat(results[0].description).contains("construction")
    }

    @Test
    fun `should search catalog items with partial match`() {
        // Arrange - create test items
        val item1 = CatalogItem(
            name = "Parafuso Phillips",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 100L,
            status = CatalogItemStatus.ACTIVE,
            description = "Screw with Phillips head"
        )
        val item2 = CatalogItem(
            name = "Parafusadeira Elétrica",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 15000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Electric screwdriver"
        )
        val item3 = CatalogItem(
            name = "Martelo",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 3000L,
            status = CatalogItemStatus.ACTIVE
        )

        catalogItemService.create(item1)
        catalogItemService.create(item2)
        catalogItemService.create(item3)

        // Act - search for partial term "paraf" which should match both items
        val results = catalogItemService.search("paraf")

        // Assert - should match items containing "paraf" in name
        assertThat(results).hasSize(2)
        assertThat(results.map { it.name }).containsExactlyInAnyOrder("Parafuso Phillips", "Parafusadeira Elétrica")
    }

    @Test
    fun `should search catalog items case-insensitively`() {
        // Arrange - create test items
        val item1 = CatalogItem(
            name = "USB-C Cable",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 1500L,
            status = CatalogItemStatus.ACTIVE
        )
        val item2 = CatalogItem(
            name = "HDMI Adapter",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 2000L,
            status = CatalogItemStatus.ACTIVE
        )

        catalogItemService.create(item1)
        catalogItemService.create(item2)

        // Act - search with lowercase
        val results = catalogItemService.search("usb-c")

        // Assert
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("USB-C Cable")
    }

    @Test
    fun `should search catalog items matching both name and description`() {
        // Arrange - create test items
        val item1 = CatalogItem(
            name = "Laptop Computer",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 500000L,
            status = CatalogItemStatus.ACTIVE,
            description = "High-performance laptop for professional use"
        )
        val item2 = CatalogItem(
            name = "Desktop Computer",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 600000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Powerful desktop for gaming and work"
        )
        val item3 = CatalogItem(
            name = "Mouse",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 5000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Computer mouse with ergonomic design"
        )

        catalogItemService.create(item1)
        catalogItemService.create(item2)
        catalogItemService.create(item3)

        // Act - search for "computer" which appears in both name and description
        val results = catalogItemService.search("computer")

        // Assert - should match all items containing "computer" in name or description
        assertThat(results).hasSize(3)
        assertThat(results.map { it.name }).containsExactlyInAnyOrder("Laptop Computer", "Desktop Computer", "Mouse")
    }

    @Test
    fun `should search catalog items with partial match in description`() {
        // Arrange - create test items
        val item1 = CatalogItem(
            name = "Screw Set",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 2000L,
            status = CatalogItemStatus.ACTIVE,
            description = "Set of various screws including Phillips and flat head"
        )
        val item2 = CatalogItem(
            name = "Nail Set",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 1500L,
            status = CatalogItemStatus.ACTIVE,
            description = "Assorted nails for construction"
        )

        catalogItemService.create(item1)
        catalogItemService.create(item2)

        // Act - search for partial term "phill" which should match item1 description
        val results = catalogItemService.search("phill")

        // Assert
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("Screw Set")
        assertThat(results[0].description).contains("Phillips")
    }

    @Test
    fun `should create catalog item with pending approval status`() {
        // Arrange
        val item = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.MATERIAL,
            priceCents = 2500L
        )

        // Act
        val created = catalogItemService.create(item)

        // Assert
        assertThat(created.id).isNotNull()
        assertThat(created.name).isEqualTo(item.name)
        assertThat(created.category).isEqualTo(item.category)
        assertThat(created.priceCents).isEqualTo(item.priceCents)
        assertThat(created.status).isEqualTo(CatalogItemStatus.PENDING_APPROVAL)
        assertThat(created.createdAt).isNotNull()
        assertThat(created.updatedAt).isNotNull()
    }

    @Test
    fun `should create catalog item with description`() {
        // Arrange
        val description = "This is a detailed description of the catalog item with multiple lines of text."
        val item = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.SERVICE,
            priceCents = 50000L,
            description = description
        )

        // Act
        val created = catalogItemService.create(item)

        // Assert
        assertThat(created.id).isNotNull()
        assertThat(created.description).isEqualTo(description)
        assertThat(created.name).isEqualTo(item.name)
    }

    @Test
    fun `should create catalog item without description`() {
        // Arrange
        val item = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.MATERIAL,
            priceCents = 3000L,
            description = null
        )

        // Act
        val created = catalogItemService.create(item)

        // Assert
        assertThat(created.id).isNotNull()
        assertThat(created.description).isNull()
    }

    @Test
    fun `should retrieve catalog item with description`() {
        // Arrange
        val description = "A comprehensive description for testing retrieval."
        val item = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.SERVICE,
            priceCents = 75000L,
            description = description
        )
        val created = catalogItemService.create(item)

        // Act
        val retrieved = catalogItemService.get(created.id!!)

        // Assert
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.description).isEqualTo(description)
        assertThat(retrieved.id).isEqualTo(created.id)
    }

    @Test
    fun `should get catalog item by id`() {
        // Arrange
        val item = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.SERVICE,
            priceCents = 150000L
        )
        val created = catalogItemService.create(item)

        // Act
        val found = catalogItemService.get(created.id!!)

        // Assert
        assertThat(found).isNotNull()
        assertThat(found!!.id).isEqualTo(created.id)
        assertThat(found.name).isEqualTo(created.name)
    }

    @Test
    fun `should return null when getting non-existent item`() {
        // Act
        val found = catalogItemService.get(UUID.randomUUID())

        // Assert
        assertThat(found).isNull()
    }

    @Test
    fun `should throw exception when creating item with blank name`() {
        // Arrange
        val item = CatalogItem(
            name = "",
            category = CatalogItemCategory.MATERIAL,
            priceCents = 1000L
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            catalogItemService.create(item)
        }
    }

    @Test
    fun `should throw exception when creating item with negative price`() {
        // Arrange
        val item = CatalogItem(
            name = uniqueName(),
            category = CatalogItemCategory.MATERIAL,
            priceCents = -100L
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            catalogItemService.create(item)
        }
    }
}


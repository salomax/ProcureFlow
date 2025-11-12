# language: en

@procurement @core-flow
Feature: Search and Enroll Materials and Services

  As a Procurement User
  I want to search a central catalog for items and enroll new ones
  So that I can quickly find what I need or formally add missing items to the system.

  @happy-path
  Scenario: Search all catalog items when no search term is provided
    Given the catalog contains multiple items
    When the user searches with an empty search term
    Then the search results should display all catalog items
    And the system should indicate that all items are available for selection

  @happy-path
  Scenario: Successfully search and find an existing item
    Given the catalog contains an item named "USB-C Cable - 1m"
    When the user searches for "USB-C Cable"
    Then the search results should display "USB-C Cable - 1m"
    And the system should indicate that the item is available for selection

  @sad-path
  Scenario: Search for an item that is not yet enrolled
    Given the catalog does not contain an item named "Customized Team T-Shirt"
    When the user searches for "Team T-Shirt"
    Then the search results should be empty
    And the system should display the option to "Enroll New Item"

  @enrollment
  Scenario Outline: Successfully enroll a new service or material
    Given the user is on the "Enroll New Item" form
    When the user provides the Name "<Name>", Category "<Category>", Price "<Price>", and Description "<Description>"
    And the user submits the form
    Then the system should confirm the enrollment of "<Name>"
    And the new item should be marked with the Status "<Status>"
    And the new item should have the Description "<Description>"

    Examples:
      | Name                       | Category | Price   | Description                                    | Status            |
      | Customized Team T-Shirt    | MATERIAL | 25.00   | High-quality cotton t-shirt with custom logo  | PENDING_APPROVAL  |
      | On-site Consulting Service | SERVICE  | 1500.00 | Professional consulting services on-site      | PENDING_APPROVAL  |



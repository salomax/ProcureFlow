# language: en

@procurement @chat @catalog @ai-assistant
Feature: Chat-Based Catalog Assistant

  As a Procurement User
  I want to interact with the catalog through natural language chat
  So that I can quickly search for items, get details, and checkout items directly using conversational commands.

  @happy-path
  Scenario: Checkout item directly via chat command
    Given the catalog contains an item named "USB-C Cable - 1m" with price 15.00
    When the user types "I need to buy 10 USB-C cables"
    Then the chat should recognize the intent to checkout items
    And the chat should search for matching items in the catalog
    And the chat should find "USB-C Cable - 1m"
    And the chat should respond with "I found 'USB-C Cable - 1m' at $15.00 each. Processing checkout for 10 units."
    And the system should process checkout for 10 units of "USB-C Cable - 1m"
    And the checkout should be completed successfully

  @happy-path
  Scenario: Search catalog items by name via chat
    Given the catalog contains items named "USB-C Cable - 1m", "USB-C Cable - 2m", and "HDMI Cable"
    When the user types "Search for USB-C cables"
    Then the chat should recognize the search intent
    And the chat should search the catalog for "USB-C"
    And the chat should respond with a natural language message listing matching items
    And the response should include "USB-C Cable - 1m" and "USB-C Cable - 2m"
    And the response should not include "HDMI Cable"

  @happy-path
  Scenario: Get item details via chat
    Given the catalog contains an item named "USB-C Cable - 1m" with description "High-quality USB-C cable", price 15.00, and status "AVAILABLE"
    When the user types "Tell me about USB-C Cable - 1m"
    Then the chat should recognize the intent to get item details
    And the chat should find the item in the catalog
    And the chat should respond with a natural language message containing:
      | Field      | Value                                    |
      | Name       | USB-C Cable - 1m                        |
      | Description| High-quality USB-C cable                 |
      | Price      | $15.00                                   |
      | Status     | AVAILABLE                                |

  @happy-path
  Scenario: Get specific item property via chat
    Given the catalog contains an item named "USB-C Cable - 1m" with price 15.00
    When the user types "What is the price of USB-C Cable - 1m?"
    Then the chat should recognize the intent to get a specific property
    And the chat should find the item in the catalog
    And the chat should respond with "The price of 'USB-C Cable - 1m' is $15.00"

  @happy-path
  Scenario: Get item status via chat
    Given the catalog contains an item named "USB-C Cable - 1m" with status "PENDING_APPROVAL"
    When the user types "What is the status of USB-C Cable - 1m?"
    Then the chat should recognize the intent to get item status
    And the chat should find the item in the catalog
    And the chat should respond with "The status of 'USB-C Cable - 1m' is PENDING_APPROVAL"

  @happy-path
  Scenario: Get item description via chat
    Given the catalog contains an item named "USB-C Cable - 1m" with description "High-quality USB-C cable with fast charging support"
    When the user types "Describe USB-C Cable - 1m"
    Then the chat should recognize the intent to get item description
    And the chat should find the item in the catalog
    And the chat should respond with "USB-C Cable - 1m: High-quality USB-C cable with fast charging support"

  @happy-path
  Scenario: Checkout item with quantity specified in natural language
    Given the catalog contains an item named "Wireless Mouse" with price 25.00
    When the user types "I need to buy 5 wireless mice"
    Then the chat should recognize the intent to checkout items
    And the chat should find "Wireless Mouse" in the catalog
    And the chat should extract the quantity "5"
    And the chat should respond with "I found 'Wireless Mouse' at $25.00 each. Processing checkout for 5 units."
    And the system should process checkout for 5 units of "Wireless Mouse"
    And the checkout should be completed successfully

  @happy-path
  Scenario: Checkout item with default quantity when not specified
    Given the catalog contains an item named "Ergonomic Keyboard" with price 75.00
    When the user types "I want to buy an ergonomic keyboard"
    Then the chat should recognize the intent to checkout items
    And the chat should find "Ergonomic Keyboard" in the catalog
    And the chat should use default quantity of 1
    And the chat should respond with "I found 'Ergonomic Keyboard' at $75.00 each. Processing checkout for 1 unit."
    And the system should process checkout for 1 unit of "Ergonomic Keyboard"
    And the checkout should be completed successfully

  @sad-path
  Scenario: Search for item that doesn't exist
    Given the catalog does not contain an item named "Custom Widget"
    When the user types "Search for Custom Widget"
    Then the chat should recognize the search intent
    And the chat should search the catalog
    And the chat should respond with "I couldn't find any items matching 'Custom Widget' in the catalog. Would you like to enroll a new item?"

  @sad-path
  Scenario: Checkout item that doesn't exist
    Given the catalog does not contain an item named "Non-existent Item"
    When the user types "I need to buy 3 Non-existent Items"
    Then the chat should recognize the intent to checkout items
    And the chat should search for matching items
    And the chat should not find any matching items
    And the chat should respond with "I couldn't find 'Non-existent Item' in the catalog. Would you like to search for similar items or enroll a new item?"

  @sad-path
  Scenario: Get details for item that doesn't exist
    Given the catalog does not contain an item named "Unknown Product"
    When the user types "Tell me about Unknown Product"
    Then the chat should recognize the intent to get item details
    And the chat should search for the item
    And the chat should not find the item
    And the chat should respond with "I couldn't find 'Unknown Product' in the catalog. Would you like to search for similar items?"

  @edge-case
  Scenario: Ambiguous item name with multiple matches
    Given the catalog contains items named "USB-C Cable - 1m", "USB-C Cable - 2m", and "USB-C Adapter"
    When the user types "I need to buy USB-C cable"
    Then the chat should recognize the intent to checkout items
    And the chat should find multiple matching items
    And the chat should respond with a natural language message asking for clarification
    And the response should list all matching items: "USB-C Cable - 1m", "USB-C Cable - 2m", and "USB-C Adapter"
    And the chat should ask "Which one would you like to checkout?"

  @edge-case
  Scenario: Natural language quantity variations
    Given the catalog contains an item named "USB-C Cable - 1m"
    When the user types "I need ten USB-C cables"
    Then the chat should recognize the quantity "ten" as 10
    And the chat should process checkout for 10 units
    And the chat should respond confirming the checkout of 10 units
    And the checkout should be completed successfully

  @edge-case
  Scenario: Chat maintains conversation context
    Given the user previously asked about "USB-C Cable - 1m"
    When the user types "I need to buy 5 of those"
    Then the chat should understand "those" refers to "USB-C Cable - 1m"
    And the chat should process checkout for 5 units of "USB-C Cable - 1m"
    And the chat should respond with "Processing checkout for 5 units of 'USB-C Cable - 1m'."
    And the checkout should be completed successfully

  @edge-case
  Scenario: Chat handles typos and variations in item names
    Given the catalog contains an item named "USB-C Cable - 1m"
    When the user types "Search for usb c cable"
    Then the chat should use fuzzy matching or similarity search
    And the chat should find "USB-C Cable - 1m"
    And the chat should respond with "I found 'USB-C Cable - 1m'. Did you mean this?"

  @edge-case
  Scenario: Multiple actions in a single message
    Given the catalog contains items named "USB-C Cable - 1m" and "Wireless Mouse"
    When the user types "I need 5 USB-C cables and tell me the price of wireless mouse"
    Then the chat should recognize multiple intents
    And the chat should process checkout for 5 units of "USB-C Cable - 1m"
    And the chat should provide the price of "Wireless Mouse"
    And the chat should respond with a natural language message addressing both requests
    And the checkout should be completed successfully


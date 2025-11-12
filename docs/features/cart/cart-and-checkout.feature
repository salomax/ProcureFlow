# language: en

@procurement @core-flow @cart
Feature: Cart and Checkout

  As a Procurement User
  I want to select items, add them to a cart, and checkout
  So that I can organize my procurement requests and confirm my selections.

  @happy-path
  Scenario: Add a single item to cart
    Given the user is viewing catalog items
    When the user selects an item "USB-C Cable - 1m"
    And the user adds the item to the cart
    Then the cart should contain 1 item
    And the cart should display "USB-C Cable - 1m"
    And the cart icon should show a badge with count "1"

  @happy-path
  Scenario: Add multiple items to cart
    Given the user is viewing catalog items
    When the user selects "USB-C Cable - 1m"
    And the user adds it to the cart
    And the user selects "Wireless Mouse"
    And the user adds it to the cart
    And the user selects "Ergonomic Keyboard"
    And the user adds it to the cart
    Then the cart should contain 3 items
    And the cart icon should show a badge with count "3"

  @happy-path
  Scenario: View cart contents
    Given the cart contains 2 items
    When the user opens the cart
    Then the cart should display all added items
    And each item should show its name, price, and quantity
    And the cart should display the total price

  @happy-path
  Scenario: Remove item from cart
    Given the cart contains 3 items
    When the user removes "Wireless Mouse" from the cart
    Then the cart should contain 2 items
    And the cart should not display "Wireless Mouse"
    And the cart icon should show a badge with count "2"

  @happy-path
  Scenario: Update item quantity in cart
    Given the cart contains "USB-C Cable - 1m" with quantity 1
    When the user increases the quantity to 3
    Then the cart should display "USB-C Cable - 1m" with quantity 3
    And the item subtotal should reflect the updated quantity

  @happy-path
  Scenario: Clear entire cart
    Given the cart contains multiple items
    When the user clears the cart
    Then the cart should be empty
    And the cart icon should not show a badge
    And the user should see a message indicating the cart is empty

  @happy-path
  Scenario: Proceed to checkout with items in cart
    Given the cart contains at least 1 item
    When the user proceeds to checkout
    Then the user should be redirected to the checkout page
    And the checkout page should display all cart items
    And the checkout page should display the total amount

  @happy-path
  Scenario: Set item quantity during checkout
    Given the user is on the checkout page
    And the cart contains "USB-C Cable - 1m" with quantity 1
    When the user sets the quantity for "USB-C Cable - 1m" to 3
    Then the checkout page should display "USB-C Cable - 1m" with quantity 3
    And the item subtotal should reflect the updated quantity
    And the total amount should reflect the updated quantity

  @happy-path
  Scenario: Complete checkout successfully
    Given the user is on the checkout page
    And the cart contains items
    When the user confirms the checkout
    Then the system should simulate a successful checkout
    And the system should log the checkout transaction
    And the user should see a confirmation message
    And the cart should be cleared
    And the user should be redirected to a confirmation page

  @sad-path
  Scenario: Attempt checkout with empty cart
    Given the cart is empty
    When the user attempts to proceed to checkout
    Then the system should prevent checkout
    And the user should see a message "Your cart is empty"
    And the user should be prompted to add items to the cart

  @sad-path
  Scenario: Attempt to add duplicate item to cart
    Given the cart already contains "USB-C Cable - 1m"
    When the user attempts to add "USB-C Cable - 1m" again
    Then the system should increase the quantity of the existing item
    And the cart should not contain duplicate entries

  @edge-case
  Scenario: Cart persists across page navigation
    Given the user has added items to the cart
    When the user navigates to a different page
    And the user returns to the catalog
    Then the cart should still contain the previously added items
    And the cart icon should show the correct item count


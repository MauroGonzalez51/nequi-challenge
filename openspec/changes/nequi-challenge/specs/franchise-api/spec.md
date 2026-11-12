# Spec Delta

## Purpose

Defines the franchise management REST API: the domain model and the nine required endpoints with their observable behavior.

## ADDED Requirements

### Requirement: Domain model
The system SHALL model a Franchise (name, list of branches), a Branch (name, belongs to one franchise, list of products) and a Product (name, numeric stock, belongs to one branch).

#### Scenario: Hierarchy is respected
- **WHEN** a product is created
- **THEN** it belongs to exactly one branch, and that branch to exactly one franchise

### Requirement: Create franchise
The system SHALL expose an endpoint to create a new franchise.

#### Scenario: Valid creation
- **WHEN** a client submits a franchise with a name
- **THEN** the franchise is persisted and returned

#### Scenario: Invalid name
- **WHEN** the name is missing or blank
- **THEN** the system responds with a client error and persists nothing

### Requirement: Add branch to franchise
The system SHALL expose an endpoint to add a branch to an existing franchise.

#### Scenario: Franchise exists
- **WHEN** a branch is added to an existing franchise
- **THEN** the branch is persisted under that franchise

#### Scenario: Franchise missing
- **WHEN** the franchise does not exist
- **THEN** the system responds with not found

### Requirement: Add product to branch
The system SHALL expose an endpoint to add a product with a name and stock to an existing branch.

#### Scenario: Branch exists
- **WHEN** a valid product is added to an existing branch
- **THEN** the product is persisted under that branch

#### Scenario: Invalid stock
- **WHEN** stock is negative or not numeric
- **THEN** the system responds with a client error

### Requirement: Remove product from branch
The system SHALL expose an endpoint to delete a product from a branch.

#### Scenario: Product exists
- **WHEN** an existing product is deleted
- **THEN** it no longer appears in the branch

#### Scenario: Product missing
- **WHEN** the product does not exist
- **THEN** the system responds with not found

### Requirement: Update product stock
The system SHALL expose an endpoint to modify a product's stock.

#### Scenario: Valid update
- **WHEN** a valid stock value is submitted for an existing product
- **THEN** the new stock is persisted and returned

### Requirement: Top-stock product per branch
The system SHALL expose an endpoint that, for a given franchise, returns the product with the highest stock for each of its branches.

#### Scenario: Multiple branches
- **WHEN** a franchise has several branches with products
- **THEN** the response contains one entry per branch with that branch's highest-stock product

#### Scenario: Branch without products
- **WHEN** a branch has no products
- **THEN** the system does not fail; the branch is omitted or reported without a product, consistently

### Requirement: Rename franchise, branch and product
The system SHALL expose three endpoints to update the name of a franchise, a branch and a product respectively.

#### Scenario: Rename existing entity
- **WHEN** a valid new name is submitted for an existing entity
- **THEN** the new name is persisted and returned

#### Scenario: Rename missing entity
- **WHEN** the entity does not exist
- **THEN** the system responds with not found

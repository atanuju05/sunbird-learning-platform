Feature: Test all scenarios of update question.

  Scenario: Update a question.
    When Updating a question Taxonomy id is numeracy and question id is temp with proper question data and metadata changes
    Then return status of update question is successful and response code is 200
    
    When Updating a question Taxonomy id is numeracy and question id is temp with proper question data and relation changes
    Then return status of update question is successful and response code is 200 

  
  Scenario: Update a question using wrong identifier.
    When Updating a question Taxonomy id is numeracy and question id is ilimi with wrong question id
    Then return status of update question is failed and response code is 404
    And return error message by update question API is Node not found

  Scenario: Update a question when Content data is not in correct format or missing.
    When Updating a question Taxonomy id is numeracy and question id is temp with question as blank
    Then return status of update question is failed and response code is 400
    And return error message by update question API is question Object is blank    
    
    When Updating a question Taxonomy id is numeracy and question id is temp with require metadata
    Then return status of update question is failed and response code is 400
    And return error message by update question API is Required Metadata code not set
    
    When Updating a question Taxonomy id is numeracy and question id is temp with invalid data type for select
    Then return status of update question is failed and response code is 400
    And return error message by update question API is Metadata status should be one of: [Draft, Review, Live, Retired, Mock]
    
    When Updating a question Taxonomy id is numeracy and question id is temp with object type not set
    Then return status of update question is failed and response code is 400
    And return error message by update question API is Object type not set for node: tempQ
    
    When Updating a question Taxonomy id is numeracy and question id is temp with wrong definition node
    Then return status of update question is failed and response code is 400
    And return error message by update question API is Definition node not found for Object Type: ilimi

    

    
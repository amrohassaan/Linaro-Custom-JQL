package com.onresolve.jira.groovy.jql

import com.atlassian.fugue.Option
import com.atlassian.jira.JiraDataType
import com.atlassian.jira.JiraDataTypes
import com.atlassian.jira.bc.user.property.DefaultUserPropertyService
import com.atlassian.jira.bc.user.search.UserSearchParams
import com.atlassian.jira.bc.user.search.UserSearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.entity.property.EntityProperty
import com.atlassian.jira.entity.property.EntityPropertyService
import com.atlassian.jira.jql.operand.QueryLiteral
import com.atlassian.jira.jql.query.QueryCreationContext
import com.atlassian.jira.jql.validator.NumberOfArgumentsValidator
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.util.MessageSet
import com.atlassian.query.clause.TerminalClause
import com.atlassian.query.operand.FunctionOperand
import groovy.json.JsonSlurper
import groovy.util.logging.Log4j

@Log4j
class Company extends AbstractScriptedJqlFunction implements JqlFunction {

    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    def userPropertyService = ComponentAccessor.getComponent(DefaultUserPropertyService.class)
    def userSearchService = ComponentAccessor.getComponent(UserSearchService.class)
    def userSearchParams = (new UserSearchParams.Builder()).allowEmptyQuery(true).includeActive(true).includeInactive(false).maxResults(100000).build()

    @Override
    MessageSet validate(ApplicationUser user, FunctionOperand operand, TerminalClause terminalClause) {
        def messageSet = new NumberOfArgumentsValidator(1, 1, getI18n()).validate(operand)

        if (messageSet.hasAnyErrors()) {
            return messageSet
        }

//        if (operand.args) {
//            def employeeType = operand.args[0] as String
//            if (!employeeType.equalsIgnoreCase("member engineer") &&
//                    !employeeType.equalsIgnoreCase("assignee") &&
//                    !employeeType.equalsIgnoreCase("employee")) {
//                messageSet.addErrorMessage(getI18n().getText("expected 'employee' or 'assignee' or 'member engineer'"))
//            }
//
//        }
        return messageSet
    }

    @Override
    List<QueryLiteral> getValues(QueryCreationContext queryCreationContext, FunctionOperand operand, TerminalClause terminalClause) {

        def companyName = ""
        if (operand.args){
            companyName = operand.args.first() as String
        }
        def allUsers = userSearchService.findUsers("", userSearchParams)
        def specificCompany = new ArrayList<ApplicationUser>()
        for (ApplicationUser emp: allUsers){
            EntityPropertyService.PropertyResult propertyValues = userPropertyService.getProperty(user, emp.key, "company")//THIS SHOULD CONTAIN MY PROPERTIES
            Option<EntityProperty> jsonValues = propertyValues.getEntityProperty()
            def value = jsonValues.getOrNull()
            if (value){
                def argType = new JsonSlurper().parseText(value.value)
                if (argType.value.equalsIgnoreCase(companyName)){
                    specificCompany.add(emp)
                }
            }
        }
        if (specificCompany){
            def literals = new ArrayList<QueryLiteral>(specificCompany.size())
            for (ApplicationUser user : specificCompany)
            {
                literals.add(new QueryLiteral(operand, user.name))
            }

            return literals
        }

    }

    @Override
    Integer getMinimumNumberOfExpectedArguments() {
        1
    }

    @Override
    Integer getMaximumNumberOfExpectedArguments(){
        1
    }

    @Override
    JiraDataType getDataType() {
        JiraDataTypes.USER
    }

    @Override
    String getDescription() {
        "Query issues assigned to an engineer from specific company"
    }

    @Override
    List<Map> getArguments() {
        [
                ["description": "Company Name ", "optional": false],
        ]
    }

    @Override
    boolean isList() {
        true
    }

    @Override
    String getFunctionName() {
        "company"
    }
}

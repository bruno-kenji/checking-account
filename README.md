## What am I
I am a Clojure API that simulates checking account features. This is a training for studying purposes only.  

## Setup
Install leiningen: https://leiningen.org/.  

## Running the API server
From the project directory, run: **lein ring server**.  
## Running tests  
From the project directory, run **lein midje**.  

## Using the API
For testing the requests, use Postman: https://www.getpostman.com/ or the method of your preference.  
Example of URL using localhost: https://localhost:3000/456/balance.  

### ACCOUNTS
GET :hosturl/accounts  
POST :hosturl/accounts  

##### POST Request Parameters  
```javascript
{"account-number":int,  
  "operations":[],  
  "balance":float}
```
operation format:  
```javascript
{"amount": float,  
  "date": "yyyy-MM-dd",  
  "description": "{action} {formatted money} at {yyyy-MM-dd}",  
  "id": int}
```

### DEBIT
POST :hosturl/:account-number/debits  

##### Request Parameters  
```javascript
{"amount":float,  
  "type":"purchase", "withdrawal" or "debit",  
  "date":"yyyy-MM-dd",  
  "other-party":"string"}
```
*other-party* is considered for "purchase" type only  

### CREDIT
POST :hosturl/:account-number/credits  

##### Request Parameters  
```javascript
{"amount":float,  
  "type":"deposit", "withdrawal" or "credit",  
  "date":"yyyy-MM-dd"}
```

### BALANCE
GET :hosturl/:account-number/balances  

### STATEMENTS
GET :hosturl/:account-number/statements?from=*yyyy-MM-dd*&to=*yyyy-MM-dd*  

### DEBTS
GET :hosturl/:account-number/debts  

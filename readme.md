# Meeting Room App

A android tool to simplify the most common/unsupported requirements of business meetings.
It interacts with the calendar to find "meetings" (AKA events), and a gmail account to send emails.

## Current Functionality

### Meeting Feedback
Give anonymous feedback on a meeting you attended. You may give feedback
(Positive/Green, Neutral/Yellow or Negative/Red). The app will compose a basic email and send it
using the configured gmail account.

## Configuration
The app require a properties fine in `src/main/res/raw/config.properties` that sets up the email
account that the app requires, as well as a the organiser email address that the app is interested
in. This `interested in email` can use sql wild cards to allow multiple emails to be selected.
Here's an example content for the `config.properties` file.

```
email=feedback@naimuri.com
email_password=password123
interested_in_email=%@naimuri.com
```

## Deployment
The requirement of the configuration file, means that deployment to the android device must be done
manually.

`WIP I hope to automate as much of this as possible with bash scripts`

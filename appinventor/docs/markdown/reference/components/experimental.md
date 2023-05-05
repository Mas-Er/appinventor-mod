---
layout: documentation
title: Experimental
---

[&laquo; Back to index](index.html)
# Experimental

Table of Contents:

* [ChatBot](#ChatBot)
* [FirebaseDB](#FirebaseDB)

## ChatBot  {#ChatBot}

The ChatBot component is a non-visible component for chatting with an AI
 chatbot. This version uses a proxy run by MIT that in turn uses the ChatGPT
 generative large language model.



### Properties  {#ChatBot-Properties}

{:.properties}

{:id="ChatBot.ApiKey" .text .ro .bo} *ApiKey*
: Property for ApiKey

{:id="ChatBot.Apikey" .text .wo .bo} *Apikey*
: An ApiKey for ChatGPT. User supplied. If provided, we will use it instead of the
 API key embedded in the chat proxy service.

 Note: We do not provide this as a DesignerProperty, it should be stored using the
 blocks, preferably using the Obfuscated Text block to provide some protection
 (not perfect protection) of the key embedded in a packaged app.

{:id="ChatBot.System" .text} *System*
: The "System" value given to ChatGPT. It is used to set the tone of a conversation. For example: "You are a funny person."

{:id="ChatBot.Token" .text .wo} *Token*
: The MIT Access token to use. MIT App Inventor will automatically fill this
 value in. You should not need to change it.

### Events  {#ChatBot-Events}

{:.events}

{:id="ChatBot.GotResponse"} GotResponse(*responseCode*{:.text},*responseText*{:.text})
: Event indicating that a request has finished and has returned data (output from ChatBot).

### Methods  {#ChatBot-Methods}

{:.methods}

{:id="ChatBot.Converse" class="method"} <i/> Converse(*question*{:.text})
: Ask a question of the Chat Bot. Successive calls will remember information from earlier in the conversation. Use the "ResetConversation" function to reset for a new conversation.

{:id="ChatBot.ResetConversation" class="method"} <i/> ResetConversation()
: Reset the current conversation, Chat bot will forget any previous conversation when resonding in the future.

## FirebaseDB  {#FirebaseDB}

The Firebase component communicates with a Web service to store
 and retrieve information.  The component has methods to
 store a value under a tag and to retrieve the value associated with
 the tag. It also possesses a listener to fire events when stored
 values are changed.

 [Additional Information](../other/firebaseIntro.html)



### Properties  {#FirebaseDB-Properties}

{:.properties}

{:id="FirebaseDB.DeveloperBucket" .text .do} *DeveloperBucket*
: Getter for the DeveloperBucket.

{:id="FirebaseDB.FirebaseToken" .text .do} *FirebaseToken*
: Getter for the FirebaseToken.

{:id="FirebaseDB.FirebaseURL" .text .do} *FirebaseURL*
: Specifies the URL for the Firebase.

 The default value is currently my private Firebase URL, but this will
 eventually changed once the App Inventor Candle plan is activated.

{:id="FirebaseDB.Persist" .boolean .wo .do} *Persist*
: If true, variables will retain their values when off-line and the App exits. Values will be uploaded to Firebase the next time the App is run while connected to the network. This is useful for applications which will gather data while not connected to the network. Note: AppendValue and RemoveFirst will not work correctly when off-line, they require a network connection.<br/><br/> <i>Note</i>: If you set Persist on any Firebase component, on any screen, it makes all Firebase components on all screens persistent. This is a limitation of the low level Firebase library. Also be aware that if you want to set persist to true, you should do so before connecting the Companion for incremental development.

{:id="FirebaseDB.ProjectBucket" .text} *ProjectBucket*
: Getter for the ProjectBucket.

### Events  {#FirebaseDB-Events}

{:.events}

{:id="FirebaseDB.DataChanged"} DataChanged(*tag*{:.text},*value*{:.any})
: Indicates that the data in the Firebase has changed.
 Launches an event with the tag and value that have been updated.

{:id="FirebaseDB.FirebaseError"} FirebaseError(*message*{:.text})
: Indicates that the communication with the Firebase signaled an error.

{:id="FirebaseDB.FirstRemoved"} FirstRemoved(*value*{:.any})
: Event triggered by the "RemoveFirst" function. The argument "value" is the object that was the first in the list, and which is now removed.

{:id="FirebaseDB.GotValue"} GotValue(*tag*{:.text},*value*{:.any})
: Indicates that a GetValue request has succeeded.

{:id="FirebaseDB.TagList"} TagList(*value*{:.list})
: Event triggered when we have received the list of known tags. Used with the "GetTagList" Function.

### Methods  {#FirebaseDB-Methods}

{:.methods}

{:id="FirebaseDB.AppendValue" class="method"} <i/> AppendValue(*tag*{:.text},*valueToAdd*{:.any})
: Append a value to the end of a list atomically. If two devices use this function simultaneously, both will be appended and no data lost.

{:id="FirebaseDB.ClearTag" class="method"} <i/> ClearTag(*tag*{:.text})
: Asks Firebase to forget (delete or set to "null") a given tag.

{:id="FirebaseDB.GetTagList" class="method"} <i/> GetTagList()
: Get the list of tags for this application. When complete a "TagList" event will be triggered with the list of known tags.

{:id="FirebaseDB.GetValue" class="method"} <i/> GetValue(*tag*{:.text},*valueIfTagNotThere*{:.any})
: GetValue asks Firebase to get the value stored under the given tag.
 It will pass valueIfTagNotThere to GotValue if there is no value stored
 under the tag.

{:id="FirebaseDB.RemoveFirst" class="method"} <i/> RemoveFirst(*tag*{:.text})
: Return the first element of a list and atomically remove it. If two devices use this function simultaneously, one will get the first element and the the other will get the second element, or an error if there is no available element. When the element is available, the "FirstRemoved" event will be triggered.

{:id="FirebaseDB.StoreValue" class="method"} <i/> StoreValue(*tag*{:.text},*valueToStore*{:.any})
: Asks Firebase to store the given value under the given tag.

{:id="FirebaseDB.Unauthenticate" class="method"} <i/> Unauthenticate()
: Unauthenticate from Firebase.

   Firebase keeps track of credentials in a cache in shared_prefs
 It will re-use these credentials as long as they are valid. Given
 That we retrieve a FirebaseToken with a version long life, this will
 effectively be forever. Shared_prefs survive an application update
 and depending on how backup is configured on a device, it might survive
 an application removal and reinstallation.

   Normally this is not a problem, however if we change the credentials
 used, for example the App author is switching from one Firebase account
 to another, or invalided their firebase.secret, this cached credential
 is invalid, but will continue to be used, which results in errors.

   This function permits us to unauthenticate, which tosses the cached
 credentials. The next time authentication is needed we will use our
 current FirebaseToken and get fresh credentials.

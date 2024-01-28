# Assignment-Local-mail-server

This program is a local SMTP (Simple Mail Transfer Protocol) server that allows one to "send" emails, read and delete mails from his inbox and verify that another user is among the server's users list. I confess that my grade was not good at this assignment (I cannot be perfect at everything) 😏🙂, but I like what I have written nevertheless, so I decided to show it to the world! The server lacks security/encryption (could not incorporate the code I found), functional mailboxes (could not implement them), logging in of different users (could not synchronize the threads) and email flagging (reached ~80% and made the mistake of deleting the code instead of commenting it out) among others.

The program was written using the [IntelliJ IDEA](https://www.jetbrains.com/idea/promo/) IDE, and we were given its basic layout beforehand \[client-server connectivity was already present (the client could send a HELO message) and also some error messages had been implemented in the ServerConnectionHandler class].

You can run it in any IDE \[I have actually switched to [Visual Studio Code](https://code.visualstudio.com/) since then] by running the server, providing a port number and then running the client and using the same port. Your email address \(johndoe@client-domain.gr) has been hardcoded. The users \"howardphilips@client-domain.gr", \"bobsalvatore@client-domain.gr", \"rogerwaters@client-domain.gr" and \"rogerfederer@client-domain.gr" have also been hardcoded, to assist in checking the VRFY command's effectiveness.

<?xml version="1.0"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV=http://schemas.xmlsoap.org/soap/envelope/ xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance>
<SOAP-ENV:Body>
	<EntryEvent>
		<ID>${id}</ID>
		<Date>${(start?number_to_datetime?string("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))}</Date>
		<Message>${title}</Message>
	</EntryEvent>
</SOAP-ENV:Body>
</SOAP-ENV:Envelope>

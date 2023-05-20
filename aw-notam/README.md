# NOTAM

https://www.faa.gov/about/initiatives/notam

https://www.faa.gov/air_traffic/publications/atpubs/notam_html/chap8_section_1.html

https://api.faa.gov/s/login/SelfRegister

https://www.notams.faa.gov/dinsQueryWeb/

https://accounts.swim.faa.gov/realms/swim/login-actions/registration?execution=4adc2591-529a-4b43-be63-2f846085ae71&client_id=swim-ui&tab_id=iowv9OFgGrQ

https://www.icao.int/Aviation-API-Data-Service/Pages/default.aspx

https://www.icao.int/safety/iStars/Pages/Get-NOTAM-Data.aspx

https://www.topcoder.com/challenges/cf1f9fda-95d5-4469-83cb-d0d16fc240f1

https://github.com/fdesjardins/notams

http://www.drorpilot.com/Notam

https://www.avdelphi.com/asearch.html?scope=notam&uid=LIRR;A1043/23&cache=1676880786

https://www.thinkaviation.net/notams-decoded/

## NOTAM Stream

https://www.theairlinepilots.com/notams-icao.php

## NOTAM Archive

https://notams.aim.faa.gov/notamSearch/nsapp.html#/details

### Example

NOTAM tutorial
learing the Notices to airman -NOTAM
NOTAM are identified by letters:

Q
A
B
C
D
E
F
G
Individual items are often omitted if unnecessary or inappropriate.

Although NOTAMs are in code, the vital information is often in plain language, or obvious from the text, as the examples indicate.

Item Q contains a comprehensive description of information contained within the NOTAM.
It consists of up to eight fields separated by a stroke(/).
This information is repeated in the text of the NOTAM, so an explanation is not given here.

Some authorities do not include Item Q in NOTAMs.
Item A is the 4-letter ICAO code for the location, e.g. LFXX indicates France;
LFFF indicates Paris FIR, or LFPG indicates Paris/Charles de Gaulle Airport.
Flight Guides give location decodes.

Item B is the 10-figure group giving in order the year, month, date and time at which any change to already published information comes into force,
e.g. 0706231050 indicates 1050 UTC on 23rd June 2007.
Alternatively the date/time group may be written in plain language, in the same order, e.g. 2007 Jun 23 1050.
WIE means With Immediate Effect.

Item C is the 10-figure group giving the year, month, date and time at which the NOTAM ceases to have effect.
Item C may be omitted if the information is permanent, or PERM (permanent) or UFN (untilfurther notice) may be inserted.

Item D gives the schedule of dates and times when the NOTAM will be active, e.g. JUN 23 24 25 26 AND 27 1050 to 1800.

Item E describes, in plain language but using simple abbreviations where appropriate,the nature of the event which is the subject of the NOTAM.

Items F and G indicate the lower and upper limit of activity of navigation warnings or airspace reservations.
If the lower limit is ground level, Item F is usually omitted, but SFC or GRD may be inserted.

EXAMPLE 1

```
Q) EGXX/QRDCA/IV/BO/W/000/080/5451N00456W014 FROM 07/06/18 14:00 TO 07/06/22 17:00
D) 18 1400-1700 19 - 22 0900-1700
E) DANGER AREA EG D402A ACTIVE ABOVE NORMAL LEVEL
F) SFC
G) 8000FT AMSL
```

Example 1 notifies the extension of the upper limit of a danger area to 8000 ft between certain times.
Item Q is included but items A, B and C are not.
Item Q is effectively a coded version of the whole NOTAM.
It is not essential for you to be able to decode this completely, but if A, B, and C are omitted, item Q is the only place you may find the position.

Item D indicates the period of activity
on the 18th between 1400 and 1700 and from 19th to 22nd between 0900 and 1700.
The month has been omitted because it is the month of issue.

Item E shows that the area concerned is UK Danger Area EG D402A, active above its normal level.
The location of the danger area is not indicated because it is a permanently active area marked on maps.

Items F and G show that it is active between surface and 8000 ft AMSL.

EXAMPLE 2

```
A) LFAD COMPIEGNE MARGNY
B) 2007 Jun 01 06:00
C) 2007 Aug 29 23:59
E) VOR/DME CPE 109.65MHZ CH33Y OUT OF SERVICE
```

Example 2 notifies that a navigation facility is being taken temporarily out of service.
Items Q, D, F and G are omitted
Item A states the location: Compeigne Margny aerodrome, ICAO indicator LFAD.
Items B and C indicate that the NOTAM is effective from 0600 UTC on 1st June 2007 to 2359 UTC on 29 August 2007.
Item E states that during the above period, VOR/DME 'CPE', operating on frequency 109.65 MHz, channel 33Y,will be out of service.

EXAMPLE 3

```
Q) EGTT/QWPLW/IV/M/W/000/130/5217N00255W008 FROM 07/06/18 07:00 TO 07/06/18 23:00
E) AUS 07-06-0531/1977/AS3 EXERCISE BLACK MOUNTAIN.
STATIC LINE AND FREEFALL PJE WI 8NM RADIUS 5217N 00255W (BYTON).
DROP CONE EXTENDS TO 4NM RAD SFC TO 6000FT,8NM RADIUS 6000-12000FT AGL.
ACFT IN DROP CONFIGURATION MAY BE UNABLE TO COMPLY WITH RULES OF THE AIR.
CONTACT 07767 238541.
F) SFC
G)FL130
```

Example 3 notifies a military parachute jumping exercise.
The important details are in plain language.
The NOTAM includes a mobile telephone number for emergency contact.

EXAMPLE 4

```
A) EGCC
B) 0108122359
C) 0109112359
D) MON-FRI 0800-2359
E) RWY 26 closed
```

Example 4 notifies the temporary closure of Runway 26 at Manchester
Item A, EGCC is the ICAO code for Manchester
Items B and C: the NOTAM is effective from 2359 on 12th August 2001 to 2359 on 11th September 2001.
Item D: the runway is closed from Monday to Friday each week within the above period, between 0800 and 2359 UTC.
Item E: Runway 26 closed.

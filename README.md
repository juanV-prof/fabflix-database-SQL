Partner 1: Juan Varela

Partner 2: Alfredo Leon

Both partners met up in person and worked on one computer. Hence, push to Github repo was done mostly by one partner's account.

Project 3 Video:

Contributions:

    - Alfredo: Tasks 1-4 along with Main parser
    - Juan: Task 5 and Stars & Cast parser.

Optimization Report (CastsParser.java):

Batch insert - At first, each actor-movie pair was inserted individually. This would cause excessive database calls and slow down our program. To make our parser more efficient, we implemented batch processing, where we insert the batch every time it reaches length of 500.

Caching movieId & starId - Previously, our code would make a sql call for every movie and star to get their id's. Instead, we introduced hashmaps to store any id's we have already called before.

Impact - Reduced our total run time for CastsParser from about 8-9 minutes to about 4-5 minutes.

Inconsistency report: Located in [report.txt](report.txt) 
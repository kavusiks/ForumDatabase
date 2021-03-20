drop table if exists ViewedCourse;
drop table if exists LikesPost;
drop table if exists UserInCourse;
drop table if exists ViewedPost;
drop table if exists TaggedStartingPost;
drop table if exists ReplyPost;
drop table if exists FollowUp;
drop table if exists StartingPost;
drop table if exists Folder;
drop table if exists Post;
drop table if exists Course;
drop table if exists User;


create table User(
Email varchar(30),
user_Password varchar(30),
Firstname varchar(30),
Lastname varchar(30),
user_Type varchar(10),
constraint user_pk primary key (Email),
constraint user_Type_check_value check (user_Type in ('Instructor', 'Student'))
);

create table Course(

CourseCode varchar(7),
Name_course varchar(50),
Term varchar(10),
Anonymousposts boolean,

constraint course_pk primary key (CourseCode)
);

create table Post(

PostNr int not null,
post_Text varchar(500),
post_Date date,
post_Time time,
CourseCode varchar(7) not null,
Email varchar(30) not null,
TypePost varchar(20),

constraint post_pk primary key (PostNr),
constraint post_fk1 foreign key (CourseCode)
	references Course(CourseCode)
		on update cascade
        on delete cascade,
constraint post_fk2 foreign key (Email)
	references User(Email)
		on update cascade
        on delete cascade,
constraint TypePost_check_value check (TypePost in ('StartingPost', 'FollowUp', 'ReplyPost'))
);


create table Folder(
FolderID int,
folder_Name varchar(30),
CourseCode varchar(7) not null,
ParentFolder int,

constraint folder_pk primary key (FolderID),
constraint folder_fk1 foreign key (ParentFolder)
	references Folder(FolderID)
		on update cascade
        on delete cascade,
constraint folder_fk2 foreign key (CourseCode)
	references Course(CourseCode)
		on update cascade
        on delete cascade
);

create table StartingPost (
PostNr int,
Title varchar(30),
FolderID int not null,
constraint startingPost_pk primary key (PostNr),
constraint startingPost_fk1 foreign key (PostNr)
	references Post(PostNr)
		on update cascade
        on delete cascade,
constraint startingPost_fk2 foreign key (FolderID)
	references Folder(FolderID)
		on update cascade
        on delete cascade
);

create table FollowUp (
PostNr int,
Resolved boolean,
FollowUpOn int not null,
constraint followUp_pk primary key (PostNr),
constraint followUp_fk1 foreign key (PostNr)
	references Post(PostNr)
		on update cascade
        on delete cascade,
constraint followUp_fk2 foreign key (FollowUpOn)
	references StartingPost(PostNr)
		on update cascade
        on delete cascade
);

create table ReplyPost (
PostNr int,
CommentOn int,
AnswerOn int,
TypeReply varchar(7),

constraint replyPost_pk primary key(PostNr),
constraint replyPost_fk1 foreign key (PostNr)
	references Post(PostNr)
		on update cascade
        on delete cascade,
constraint replyPost_fk2 foreign key (CommentOn)
	references FollowUp(PostNr)
		on update cascade
        on delete cascade,
constraint replyPost_fk3 foreign key (AnswerOn)
	references StartingPost(PostNr)
		on update cascade
        on delete cascade,
constraint TypeReply_check_value check (TypeReply in ('Comment', 'Answer'))
);

create table TaggedStartingPost (
PostNr int,
Tag varchar(30),
constraint taggedStartingPost_pk primary key (PostNr, Tag),
constraint taggedStartingPost_fk foreign key (PostNr)
	references Post(PostNr)
		on update cascade
        on delete cascade,
constraint Tag_check_value check (Tag in ('questions', 'announcements', 'homework',
'homework solutions', 'lectures notes', 'general announcements'))
);

create table LikesPost (
Email varchar(30),
PostNr int,
constraint likesPost_pk primary key (Email, PostNr),
constraint likesPost_fk1 foreign key (Email)
	references User(Email)
		on update cascade
        on delete cascade,
constraint likesPost_fk2 foreign key (PostNr)
	references Post(PostNr)
		on update cascade
        on delete cascade
);

create table ViewedPost (
Email varchar(30),
PostNr int,
constraint viewedPost primary key (Email, PostNr),
constraint viewedPost_fk1 foreign key (Email)
	references User(Email)
		on update cascade
        on delete cascade,
constraint viewedPost_fk2 foreign key (PostNr)
	references StartingPost(PostNr)
		on update cascade
        on delete cascade
);

create table UserInCourse (
Email varchar(30),
CourseCode varchar(7),
constraint userInCourse_pk primary key (Email, CourseCode),
constraint userInCourse_fk1 foreign key (Email)
	references User(Email)
		on update cascade
        on delete cascade,
constraint userInCourse_fk2 foreign key (CourseCode)
	references Course(CourseCode)
		on update cascade
        on delete cascade
);

create table ViewedCourse (
Email varchar(30),
CourseCode varchar(7),
ViewedCourse_Date date,
constraint viewedCourse_pk primary key (Email, CourseCode,ViewedCourse_Date),
constraint viewedCourse_fk1 foreign key (Email)
	references User(Email)
		on update cascade
        on delete cascade,
constraint viewedCourse_fk2 foreign key (CourseCode)
	references Course(CourseCode)
		on update cascade
        on delete cascade
);

insert into User values("OlaNordmann@gmail.com", "bestePassord123", "Ola", "Nordmann", "Student");
insert into User values("PerPaulsen@hotmail.com", "bestePassord123", "Per", "Paulsen", "Instructor");
insert into User values("KariNordmann@gmail.com", "bestePassord123", "Kari", "Nordmann", "Instructor");
insert into User values("SiriPaulsen@hotmail.com", "bestePassord123", "Siri", "Paulsen", "Student");
insert into Course Values("TDT4145", "Datamodellering og databasesystemer", "Spring", false);
insert into UserInCourse Values("OlaNordmann@gmail.com", "TDT4145");
insert into UserInCourse Values("PerPaulsen@hotmail.com", "TDT4145");
insert into UserInCourse Values("KariNordmann@gmail.com", "TDT4145");
insert into UserInCourse Values("SiriPaulsen@hotmail.com", "TDT4145");
insert into Post Values(1, "Dette er første post", "2008-11-11", "13:23:44","TDT4145", "OlaNordmann@gmail.com", "StartingPost");
insert into Post Values(2, "Dette er andre post", "2008-11-12", "13:33:44","TDT4145", "KariNordmann@gmail.com", "StartingPost");
insert into Post Values(3, "Dette er tredje post", "2008-11-12", "13:35:44","TDT4145", "KariNordmann@gmail.com", "StartingPost");
insert into Folder Values(1, "Exam", "TDT4145", null);
insert into Folder Values(2, "Solutions", "TDT4145", 1);
insert into StartingPost Values(1,"Første tittel", 1);
insert into StartingPost Values(2,"Andre tittel", 1);
insert into ViewedPost Values("KariNordmann@gmail.com", 1);
insert into ViewedPost Values("SiriPaulsen@hotmail.com", 1);
insert into ViewedPost Values("SiriPaulsen@hotmail.com", 2);
insert into ViewedPost Values("PerPaulsen@hotmail.com", 1);
insert into ViewedPost Values("PerPaulsen@hotmail.com", 2)
;

/* DETTE ER SÅNN VI SKREV??
Select uc1.Email, count(vp1.PostNr) as ReadPost 
from UserInCourse as uc1 left outer join (
ViewedPost as vp1 inner join  Post as p1 on vp1.PostNr = p1.PostNr) on uc1.Email = vp1.Email
where p1.CourseCode = "TDT4145"
group by uc1.Email;*/


/*where Post.CourseCode="TDT4145";

/*Select * 
from ViewedPost as vp1 inner join (Select * 
															from Post As p1
                                                            where p1.CourseCode='TDT4145') on p1.PostNr = vp1.PostNr;*/
                                                            
                                                            

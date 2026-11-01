from .base_repository import TopicRepository
class UserRepository(TopicRepository):
    def __init__(self,client): super().__init__(client,"user")

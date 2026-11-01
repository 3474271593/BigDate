from .base_repository import TopicRepository
class GoodsRepository(TopicRepository):
    def __init__(self,client): super().__init__(client,"goods")

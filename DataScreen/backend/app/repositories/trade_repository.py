from .base_repository import TopicRepository
class TradeRepository(TopicRepository):
    def __init__(self,client): super().__init__(client,"trade")
